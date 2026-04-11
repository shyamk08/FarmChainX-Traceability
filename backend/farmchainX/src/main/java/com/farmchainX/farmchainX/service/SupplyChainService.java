package com.farmchainX.farmchainX.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.farmchainX.farmchainX.model.SupplyChainLog;
import com.farmchainX.farmchainX.repository.SupplyChainLogRepository;
import com.farmchainX.farmchainX.util.HashUtil;

@Service
public class SupplyChainService {

    private final SupplyChainLogRepository supplyChainLogRepository;

    public SupplyChainService(SupplyChainLogRepository supplyChainLogRepository) {
        this.supplyChainLogRepository = supplyChainLogRepository;
    }

    @Transactional
    public SupplyChainLog addLog(Long productId, Long fromUserId, Long toUserId,
                                 String location, String notes) {
        return addLog(productId, fromUserId, toUserId, location, notes, null);
    }

    @Transactional
    public SupplyChainLog addLog(Long productId, Long fromUserId, Long toUserId,
                                 String location, String notes, String createdBy) {

        Optional<SupplyChainLog> lastLogOpt =
                supplyChainLogRepository.findTopByProductIdOrderByTimestampDesc(productId);
        String prevHash = lastLogOpt.map(SupplyChainLog::getHash).orElse("");

        SupplyChainLog newLog = new SupplyChainLog();
        newLog.setProductId(productId);
        newLog.setFromUserId(fromUserId);
        newLog.setToUserId(toUserId);
        newLog.setLocation(location);
        newLog.setNotes(notes);
        newLog.setTimestamp(LocalDateTime.now());
        newLog.setPrevHash(prevHash);

        // ✅ Auto-assign createdBy if not provided
        String autoBy;
        if (createdBy != null && !createdBy.isBlank()) {
            autoBy = createdBy;
        } else if (fromUserId == null && toUserId != null) {
            autoBy = "Distributor";
        } else if (fromUserId != null && toUserId != null && fromUserId.equals(toUserId)) {
            autoBy = "Distributor";
        } else if (fromUserId != null && toUserId != null && !fromUserId.equals(toUserId)) {
            autoBy = "Retailer";
        } else {
            autoBy = "Farmer";
        }
        newLog.setCreatedBy(autoBy);

        if (fromUserId == null && toUserId != null) {
            newLog.setConfirmed(true);
            newLog.setConfirmedAt(LocalDateTime.now());
            newLog.setConfirmedById(toUserId);
        } else if (fromUserId != null && toUserId != null && fromUserId.equals(toUserId)) {
            newLog.setConfirmed(true);
            newLog.setConfirmedAt(LocalDateTime.now());
            newLog.setConfirmedById(toUserId);
        } else {
            newLog.setConfirmed(false);
        }

        newLog.setRejected(false);
        newLog.setHash(HashUtil.computeHash(newLog, prevHash));

        return supplyChainLogRepository.save(newLog);
    }

    @Transactional
    public SupplyChainLog confirmReceipt(Long productId, Long retailerId,
                                         String location, String notes) {
        return confirmReceipt(productId, retailerId, location, notes, null);
    }

    @Transactional
    public SupplyChainLog confirmReceipt(Long productId, Long retailerId,
                                         String location, String notes, String createdBy) {

        Optional<SupplyChainLog> lastLogOpt =
                supplyChainLogRepository.findTopByProductIdOrderByTimestampDesc(productId);

        if (lastLogOpt.isEmpty()) {
            throw new RuntimeException("No previous tracking record found for this product.");
        }

        SupplyChainLog lastLog = lastLogOpt.get();

        if (lastLog.getToUserId() == null || !lastLog.getToUserId().equals(retailerId) || lastLog.getFromUserId() == null) {
            throw new RuntimeException("This product has not been handed over to you by the distributor yet.");
        }

        if (lastLog.isConfirmed()) {
            throw new RuntimeException("This product is already confirmed as received.");
        }

        String prevHash = lastLog.getHash();

        SupplyChainLog receiptLog = new SupplyChainLog();
        receiptLog.setProductId(productId);
        receiptLog.setFromUserId(lastLog.getFromUserId());
        receiptLog.setToUserId(retailerId);
        receiptLog.setLocation(location);
        receiptLog.setNotes((notes == null || notes.isEmpty()) ? "Product received by retailer" : notes);
        receiptLog.setTimestamp(LocalDateTime.now());
        receiptLog.setPrevHash(prevHash);
        receiptLog.setConfirmed(true);
        receiptLog.setRejected(false);
        receiptLog.setConfirmedAt(LocalDateTime.now());
        receiptLog.setConfirmedById(retailerId);

        // ✅ Auto-fill createdBy if missing
        receiptLog.setCreatedBy(
                (createdBy != null && !createdBy.isBlank()) ? createdBy : "Retailer"
        );

        receiptLog.setHash(HashUtil.computeHash(receiptLog, prevHash));

        return supplyChainLogRepository.save(receiptLog);
    }

    @Transactional(readOnly = true)
    public List<SupplyChainLog> getLogsByProduct(Long productId) {
        List<SupplyChainLog> logs = supplyChainLogRepository.findByProductIdOrderByTimestampAsc(productId);

        // ✅ Auto-correct missing createdBy in old records at read time (no DB write)
        for (SupplyChainLog log : logs) {
            if (log.getCreatedBy() == null || log.getCreatedBy().isBlank()) {
                if (log.getFromUserId() == null && log.getToUserId() != null) {
                    log.setCreatedBy("Distributor");
                } else if (log.getFromUserId() != null && log.getToUserId() != null && log.getFromUserId().equals(log.getToUserId())) {
                    log.setCreatedBy("Distributor");
                } else if (log.getConfirmedById() != null && log.isConfirmed()) {
                    log.setCreatedBy("Retailer");
                } else {
                    log.setCreatedBy("Farmer");
                }
            }
        }
        return logs;
    }

    @Transactional(readOnly = true)
    public boolean verifyChain(Long productId) {
        List<SupplyChainLog> logs = getLogsByProduct(productId);
        String prevHash = "";
        for (SupplyChainLog log : logs) {
            String recomputed = HashUtil.computeHash(log, prevHash);
            if (!recomputed.equals(log.getHash())) {
                return false;
            }
            prevHash = log.getHash();
        }
        return true;
    }

    @Transactional(readOnly = true)
    public Page<SupplyChainLog> getPendingConfirmations(Long retailerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return supplyChainLogRepository.findPendingForRetailer(retailerId, pageable);
    }
}
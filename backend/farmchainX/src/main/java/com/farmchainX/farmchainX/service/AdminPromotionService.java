package com.farmchainX.farmchainX.service;

import com.farmchainX.farmchainX.model.*;
import com.farmchainX.farmchainX.repository.AdminPromotionRequestRepository;
import com.farmchainX.farmchainX.repository.RoleRepository;
import com.farmchainX.farmchainX.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminPromotionService {

    private final AdminPromotionRequestRepository requestRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;

    public AdminPromotionService(AdminPromotionRequestRepository requestRepo,
                                 UserRepository userRepo,
                                 RoleRepository roleRepo) {
        this.requestRepo = requestRepo;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
    }

    @Transactional
    public AdminPromotionRequest requestAdminAccess(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // already admin?
        if (user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()))) {
            throw new RuntimeException("User is already an admin");
        }

        // check any existing request (even if NULL flags)
        List<AdminPromotionRequest> existing = requestRepo.findAll();
        boolean hasPending = existing.stream().anyMatch(r ->
            r.getUser().getId().equals(userId) &&
            (r.isApproved() == false || r.isApproved() == Boolean.FALSE) &&
            (r.isRejected() == false || r.isRejected() == Boolean.FALSE)
        );
        if (hasPending) {
            throw new RuntimeException("You already have a pending admin request");
        }

        // create new clean request
        AdminPromotionRequest req = new AdminPromotionRequest();
        req.setUser(user);
        req.setApproved(false);
        req.setRejected(false);
        req.setRequestedAt(LocalDateTime.now());

        return requestRepo.save(req);
    }



    public List<AdminPromotionRequest> getPendingRequests() {
        return requestRepo.findByApprovedFalseAndRejectedFalse();  // NOW WORKS
    }

    @Transactional
    public String approveRequest(Long requestId, User currentAdmin) {
        AdminPromotionRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (req.isApproved() || req.isRejected()) {
            throw new RuntimeException("Already processed");
        }

        User target = req.getUser();
        Role adminRole = roleRepo.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

        target.getRoles().add(adminRole);
        userRepo.save(target);

        req.setApproved(true);
        req.setApprovedBy(currentAdmin);
        req.setProcessedAt(LocalDateTime.now());
        requestRepo.save(req);

        return target.getEmail() + " is now ADMIN";
    }

    @Transactional
    public String rejectRequest(Long requestId) {
        AdminPromotionRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        req.setRejected(true);
        req.setProcessedAt(LocalDateTime.now());
        requestRepo.save(req);

        return "Request rejected";
    }
}
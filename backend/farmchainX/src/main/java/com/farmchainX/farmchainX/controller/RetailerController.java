package com.farmchainX.farmchainX.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.farmchainX.farmchainX.model.Order;
import com.farmchainX.farmchainX.model.User;
import com.farmchainX.farmchainX.repository.OrderRepository;
import com.farmchainX.farmchainX.repository.SupplyChainLogRepository;
import com.farmchainX.farmchainX.repository.UserRepository;
import com.farmchainX.farmchainX.repository.ProductRepository;
import com.farmchainX.farmchainX.repository.DispatchOfferRepository;
import com.farmchainX.farmchainX.repository.RetailerInventoryRepository;
import com.farmchainX.farmchainX.model.SupplyChainLog;
import com.farmchainX.farmchainX.model.Product;
import com.farmchainX.farmchainX.model.DispatchOffer;
import com.farmchainX.farmchainX.model.RetailerInventory;
import com.farmchainX.farmchainX.util.HashUtil;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/retailer")
@PreAuthorize("hasRole('RETAILER')")
public class RetailerController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SupplyChainLogRepository supplyChainLogRepository;
    private final ProductRepository productRepository;
    private final DispatchOfferRepository dispatchOfferRepository;
    private final RetailerInventoryRepository retailerInventoryRepository;

    public RetailerController(UserRepository userRepository, OrderRepository orderRepository,
            SupplyChainLogRepository supplyChainLogRepository, ProductRepository productRepository,
            DispatchOfferRepository dispatchOfferRepository,
            RetailerInventoryRepository retailerInventoryRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.supplyChainLogRepository = supplyChainLogRepository;
        this.productRepository = productRepository;
        this.dispatchOfferRepository = dispatchOfferRepository;
        this.retailerInventoryRepository = retailerInventoryRepository;
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Long retailerId = user.getId();

        // 1. Inventory Value (Real calculation)
        List<SupplyChainLog> logs = supplyChainLogRepository.findByToUserIdAndConfirmedTrue(retailerId);
        // Deduplicate to get currently held items
        Map<Long, SupplyChainLog> latestReceiptByProduct = new HashMap<>();
        for (SupplyChainLog log : logs) {
            // We only care if we are the LATEST holder.
            // But findByToUserId... returns all history.
            // We need to check if there is a NEWER log where FromUserID = retailerId.
            // This is expensive if we do it for every log.
            // OPTIMIZATION: Filter in memory for MVP.
            if (!latestReceiptByProduct.containsKey(log.getProductId())) {
                latestReceiptByProduct.put(log.getProductId(), log);
            }
        }

        // Filter out items we sold
        long currentItemsCount = latestReceiptByProduct.values().stream().filter(log -> {
            // Check if we sold it
            return !supplyChainLogRepository.existsByFromUserIdAndProductId(retailerId, log.getProductId());
        }).count();

        double inventoryValue = currentItemsCount * 500.0; // Still estimating price if product lookup is slow, or we
                                                           // can fetch.

        // 2. Open POs (Status != Delivered)
        long openPOs = orderRepository.countByRetailerIdAndStatusNot(retailerId, "Delivered");

        // 3. Incoming Shipments (Pending confirmations)
        // reuse logic for "pending checks" - find logs where toUser=me and
        // confirmed=false
        long incoming = supplyChainLogRepository.countPendingForRetailer(retailerId);

        // 4. Low Stock
        long lowStock = 0; // consistent default

        Map<String, Object> stats = new HashMap<>();
        stats.put("inventoryValue", inventoryValue);
        stats.put("openPOs", openPOs);
        stats.put("incomingShipments", incoming);
        stats.put("lowStock", lowStock);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getRecentOrders(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        // Get top 5 recent orders
        return ResponseEntity
                .ok(orderRepository.findByRetailerIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 5)));
    }

    @GetMapping("/sales-chart")
    public ResponseEntity<?> getSalesChartData(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Long retailerId = user.getId();

        // Logic: Sales are items where 'fromUserId' is the retailer.
        // We want to group by day for the last 7 days.

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate sevenDaysAgo = today.minusDays(6);

        // Fetch logs where fromUser is me, after sevenDaysAgo
        List<SupplyChainLog> salesLogs = supplyChainLogRepository.findByFromUserId(retailerId).stream()
                .filter(l -> l.getTimestamp().toLocalDate().isAfter(sevenDaysAgo.minusDays(1)))
                .collect(Collectors.toList());

        Map<java.time.LocalDate, Double> dailySales = new java.util.TreeMap<>();
        // Initialize map
        for (int i = 0; i < 7; i++) {
            dailySales.put(sevenDaysAgo.plusDays(i), 0.0);
        }

        for (SupplyChainLog log : salesLogs) {
            java.time.LocalDate date = log.getTimestamp().toLocalDate();
            if (dailySales.containsKey(date)) {
                // Calculate value (mock price/quantity if not in log)
                // Ideally we fetch product price.
                double value = 100.0; // Default count per sale or weight
                dailySales.put(date, dailySales.get(date) + value);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("labels", dailySales.keySet().stream()
                .map(d -> d.format(java.time.format.DateTimeFormatter.ofPattern("MMM days"))) // unexpected pattern
                                                                                              // "days"? "MMM dd"
                .map(d -> d.replace("days", "")) // "MMM dd"
                // actually let's just use standard pattern "MMM dd"
                .collect(Collectors.toList()));

        // Fix label formatting
        List<String> labels = dailySales.keySet().stream()
                .map(d -> d.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd")))
                .collect(Collectors.toList());

        data.put("labels", labels);
        data.put("values", dailySales.values());

        return ResponseEntity.ok(data);
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> getInventory(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Long retailerId = user.getId();

        List<SupplyChainLog> logs = supplyChainLogRepository.findByToUserIdAndConfirmedTrue(retailerId);

        // Filter to items currently owned (not sold)
        // Group by productId, taking the LATEST log to me
        Map<Long, SupplyChainLog> latestReceiptByProduct = new HashMap<>(); // ProductID -> Log
        for (SupplyChainLog log : logs) {
            // Because FindByToUserId can return multiple times if I bought same product
            // twice?
            // Actually ProductID is unique per physical item in this design usually?
            // If ProductID refers to a "Batch" it might be unique. Assuming ProductID is
            // unique ITEM.
            // If I receive it, I have it.
            // Logic: I have it IF (I received it) AND (I haven't sent it away).
            if (!latestReceiptByProduct.containsKey(log.getProductId())) {
                latestReceiptByProduct.put(log.getProductId(), log);
            }
        }

        List<Map<String, Object>> inventory = latestReceiptByProduct.values().stream()
                .filter(log -> !supplyChainLogRepository.existsByFromUserIdAndProductId(retailerId, log.getProductId()))
                .map(log -> {
                    Product p = productRepository.findById(log.getProductId()).orElse(null);
                    Map<String, Object> item = new HashMap<>();
                    if (p != null) {
                        item.put("productId", p.getPublicUuid());
                        item.put("name", p.getCropName());
                        item.put("batchId", "BATCH-" + p.getId());
                        item.put("qtyOnHand", 100); // Quantity logic still mocked as "Items" are usually singular
                                                    // tracked here or need quantity field
                        item.put("unit", "kg");
                        item.put("costPrice", p.getPrice());
                        item.put("sellPrice", p.getPrice() * 1.5);
                        item.put("expiryDate", p.getHarvestDate().plusDays(10));
                        item.put("supplier", log.getCreatedBy());
                        item.put("status", "In Stock");
                    }
                    return item;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/shipments")
    public ResponseEntity<?> getShipments(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        // Fetch PENDING logs (confirmed = false)
        List<SupplyChainLog> logs = supplyChainLogRepository.findByToUserId(user.getId())
                .stream().filter(l -> !l.isConfirmed()).collect(Collectors.toList());

        List<Map<String, Object>> shipments = logs.stream().map(log -> {
            Map<String, Object> s = new HashMap<>();
            s.put("id", log.getId());
            s.put("productId", log.getProductId());
            s.put("createdBy", log.getCreatedBy());
            s.put("timestamp", log.getTimestamp());
            s.put("status", "Pending");
            return s;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(shipments);
    }

    @GetMapping("/orders/all")
    public ResponseEntity<List<Order>> getAllOrders(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(orderRepository.findByRetailerIdOrderByCreatedAtDesc(user.getId()));
    }

    @GetMapping("/provenance/{uuid}")
    public ResponseEntity<?> getProvenanceByUuid(@PathVariable String uuid) {
        Product product = productRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<SupplyChainLog> logs = supplyChainLogRepository.findByProductIdOrderByTimestampAsc(product.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("chain", logs);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/sell")
    @Transactional
    public ResponseEntity<?> sellProduct(@RequestBody Map<String, String> payload, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        String productIdStr = payload.get("productId");

        // Try to find product by numeric ID first, then by UUID
        Product product = null;
        try {
            // Try as numeric ID
            Long productId = Long.parseLong(productIdStr);
            product = productRepository.findById(productId).orElse(null);
        } catch (NumberFormatException e) {
            // Not a number, try as UUID
            product = productRepository.findByPublicUuid(productIdStr).orElse(null);
        }

        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Product not found"));
        }

        // Update RetailerInventory - reduce quantity when selling
        try {
            java.util.Optional<RetailerInventory> inventoryOpt = retailerInventoryRepository
                    .findByRetailerIdAndProductId(user.getId(), product.getId());

            if (inventoryOpt.isPresent()) {
                RetailerInventory inventory = inventoryOpt.get();
                Double currentQty = inventory.getQuantity();

                if (currentQty != null && currentQty > 0) {
                    // Reduce quantity by 1 unit
                    inventory.setQuantity(currentQty - 1.0);

                    // setQuantity automatically updates status based on quantity
                    retailerInventoryRepository.save(inventory);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Product is out of stock"));
                }
            }
        } catch (Exception e) {
            // Log error but continue with supply chain log creation
            System.err.println("[Inventory Update Error] " + e.getMessage());
        }

        // Create supply chain log for sale to consumer
        SupplyChainLog log = new SupplyChainLog();
        log.setProductId(product.getId());
        log.setFromUserId(user.getId());
        // No ToUserId means it is sold to consumer/out of chain
        log.setToUserId(null);
        log.setAction("SOLD");
        log.setTimestamp(java.time.LocalDateTime.now());
        log.setCreatedBy(user.getName() + " (Retailer)");
        log.setConfirmed(true);
        log.setLocation("Retail Store");
        log.setNotes("Sold to consumer");

        supplyChainLogRepository.save(log);

        return ResponseEntity.ok(Map.of("message", "Product sold successfully"));
    }

    @GetMapping("/dispatch-offers")
    public ResponseEntity<?> getAvailableOffers(Principal principal) {
        // Get all pending dispatch offers
        List<DispatchOffer> pendingOffers = dispatchOfferRepository.findByStatusOrderByCreatedAtDesc("PENDING");

        List<Map<String, Object>> offers = pendingOffers.stream().map(offer -> {
            Map<String, Object> offerData = new HashMap<>();
            offerData.put("offerId", offer.getId());
            offerData.put("productId", offer.getProductId());
            offerData.put("distributorId", offer.getDistributorId());
            offerData.put("location", offer.getLocation());
            offerData.put("notes", offer.getNotes());
            offerData.put("createdAt", offer.getCreatedAt());
            offerData.put("status", offer.getStatus());

            // Fetch product details
            try {
                Product p = productRepository.findById(offer.getProductId()).orElse(null);
                if (p != null) {
                    offerData.put("cropName", p.getCropName());
                    offerData.put("qualityGrade", p.getQualityGrade());
                    offerData.put("price", p.getPrice());
                    offerData.put("imagePath", p.getImagePath());
                }
            } catch (Exception e) {
                // Ignore if product not found
            }

            // Fetch distributor details
            try {
                User distributor = userRepository.findById(offer.getDistributorId()).orElse(null);
                if (distributor != null) {
                    offerData.put("distributorName", distributor.getName());
                    offerData.put("distributorEmail", distributor.getEmail());
                }
            } catch (Exception e) {
                // Ignore if distributor not found
            }

            return offerData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(offers);
    }

    @PostMapping("/accept-offer/{offerId}")
    @Transactional
    public ResponseEntity<?> acceptOffer(@PathVariable Long offerId, @RequestBody Map<String, String> payload,
            Principal principal) {
        try {
            User retailer = userRepository.findByEmail(principal.getName()).orElseThrow();
            String location = payload.getOrDefault("location", "Retailer Warehouse");

            // Find the offer
            DispatchOffer offer = dispatchOfferRepository.findById(offerId)
                    .orElseThrow(() -> new RuntimeException("Offer not found"));

            // Check if still pending (first-come-first-served)
            if (!"PENDING".equals(offer.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "This offer has already been accepted by another retailer"));
            }

            // Update offer status
            offer.setStatus("ACCEPTED");
            offer.setAcceptedBy(retailer.getId());
            offer.setAcceptedAt(java.time.LocalDateTime.now());
            dispatchOfferRepository.save(offer);

            // Create supply chain log for handover
            User distributor = userRepository.findById(offer.getDistributorId()).orElseThrow();

            SupplyChainLog lastLog = supplyChainLogRepository
                    .findTopByProductIdOrderByTimestampDesc(offer.getProductId())
                    .orElse(null);

            SupplyChainLog handoverLog = new SupplyChainLog();
            handoverLog.setProductId(offer.getProductId());
            handoverLog.setFromUserId(offer.getDistributorId());
            handoverLog.setToUserId(retailer.getId());
            handoverLog.setAction("HANDOVER");
            handoverLog.setLocation(location);
            handoverLog.setNotes("Accepted dispatch offer - " + offer.getNotes());
            handoverLog.setCreatedBy(distributor.getName() + " (Distributor)");
            handoverLog.setConfirmed(true); // Auto-confirm on accept
            handoverLog.setConfirmedAt(java.time.LocalDateTime.now());
            handoverLog.setConfirmedById(retailer.getId());
            handoverLog.setTimestamp(java.time.LocalDateTime.now());

            if (lastLog != null) {
                handoverLog.setPrevHash(lastLog.getHash());
                handoverLog.setHash(HashUtil.computeHash(handoverLog, lastLog.getHash()));
            } else {
                handoverLog.setPrevHash("");
                handoverLog.setHash(HashUtil.computeHash(handoverLog, ""));
            }

            supplyChainLogRepository.save(handoverLog);

            // Create or update RetailerInventory entry
            Product product = productRepository.findById(offer.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            RetailerInventory inventory = retailerInventoryRepository
                    .findByRetailerIdAndProductId(retailer.getId(), offer.getProductId())
                    .orElse(new RetailerInventory());

            inventory.setRetailerId(retailer.getId());
            inventory.setProductId(offer.getProductId());
            inventory.setSourceDistributorId(offer.getDistributorId());
            inventory.setPricePerUnit(product.getPrice() != null ? product.getPrice() * 1.25 : 100.0); // 25% markup
            inventory.setReceivedDate(java.time.LocalDateTime.now());

            // Set quantity - if already exists, add to it, otherwise set default
            if (inventory.getQuantity() == null) {
                inventory.setQuantity(100.0); // Default quantity in kg
            } else {
                inventory.setQuantity(inventory.getQuantity() + 100.0);
            }

            retailerInventoryRepository.save(inventory);

            return ResponseEntity.ok(Map.of(
                    "message", "Dispatch offer accepted successfully and added to inventory",
                    "productId", offer.getProductId(),
                    "offerId", offerId));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reject-offer/{offerId}")
    @Transactional
    public ResponseEntity<?> rejectOffer(@PathVariable Long offerId, Principal principal) {
        try {
            User retailer = userRepository.findByEmail(principal.getName()).orElseThrow();

            // Find the offer
            DispatchOffer offer = dispatchOfferRepository.findById(offerId)
                    .orElseThrow(() -> new RuntimeException("Offer not found"));

            // Check if still pending
            if (!"PENDING".equals(offer.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "This offer is no longer pending"));
            }

            // Update offer status to REJECTED
            offer.setStatus("REJECTED");
            offer.setAcceptedBy(retailer.getId()); // Track who rejected it
            offer.setAcceptedAt(java.time.LocalDateTime.now());
            dispatchOfferRepository.save(offer);

            return ResponseEntity.ok(Map.of(
                    "message", "Dispatch offer rejected successfully",
                    "offerId", offerId));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/orders/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();

        Long supplierId = Long.valueOf(String.valueOf(payload.get("supplierId")));
        // We might want productId too if we link it specifically, but Order model just
        // summarizes items
        int items = Integer.parseInt(String.valueOf(payload.get("quantity")));
        double total = Double.parseDouble(String.valueOf(payload.get("total")));

        Order order = new Order();
        order.setRetailerId(user.getId());
        order.setSupplierId(supplierId);
        order.setItems(items);
        order.setTotalAmount(total);
        order.setStatus("Processing");

        orderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "Order created successfully", "orderId", order.getId()));
    }
}

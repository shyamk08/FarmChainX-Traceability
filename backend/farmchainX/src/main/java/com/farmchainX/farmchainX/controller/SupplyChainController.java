package com.farmchainX.farmchainX.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.farmchainX.farmchainX.model.SupplyChainLog;
import com.farmchainX.farmchainX.model.User;
import com.farmchainX.farmchainX.repository.SupplyChainLogRepository;
import com.farmchainX.farmchainX.repository.UserRepository;
import com.farmchainX.farmchainX.service.SupplyChainService;
import com.farmchainX.farmchainX.service.ProductService;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import com.farmchainX.farmchainX.model.DispatchOffer;
import com.farmchainX.farmchainX.repository.DispatchOfferRepository;
import com.farmchainX.farmchainX.model.Product;
import com.farmchainX.farmchainX.repository.ProductRepository;
import com.farmchainX.farmchainX.repository.RetailerInventoryRepository;

@RestController
@RequestMapping("/api/track")
public class SupplyChainController {

        private final SupplyChainService supplyChainService;
        private final UserRepository userRepository;
        private final SupplyChainLogRepository supplyChainLogRepository;
        private final ProductService productService;
        private final DispatchOfferRepository dispatchOfferRepository;
        private final ProductRepository productRepository;
        private final RetailerInventoryRepository retailerInventoryRepository;

        public SupplyChainController(
                        SupplyChainService supplyChainService,
                        UserRepository userRepository,
                        SupplyChainLogRepository supplyChainLogRepository,
                        ProductService productService,
                        DispatchOfferRepository dispatchOfferRepository,
                        ProductRepository productRepository,
                        RetailerInventoryRepository retailerInventoryRepository) {
                this.supplyChainService = supplyChainService;
                this.userRepository = userRepository;
                this.supplyChainLogRepository = supplyChainLogRepository;
                this.productService = productService;
                this.dispatchOfferRepository = dispatchOfferRepository;
                this.productRepository = productRepository;
                this.retailerInventoryRepository = retailerInventoryRepository;
        }

        @PostMapping("/update-chain")
        @PreAuthorize("hasAnyRole('DISTRIBUTOR','RETAILER')")
        @Transactional
        public ResponseEntity<?> updateChain(@RequestBody Map<String, Object> payload, Principal principal) {
                logDebug("updateChain called with payload: " + payload);
                try {
                        Long productId = Long.valueOf(String.valueOf(payload.get("productId")));
                        String location = String.valueOf(payload.get("location")).trim();
                        String notes = payload.containsKey("notes") && payload.get("notes") != null
                                        ? String.valueOf(payload.get("notes")).trim()
                                        : "";
                        Long toUserId = payload.containsKey("toUserId") && payload.get("toUserId") != null
                                        ? Long.valueOf(String.valueOf(payload.get("toUserId")))
                                        : null;

                        if (location.isBlank()) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(Map.of("error", "Location is required"));
                        }

                        User currentUser = userRepository.findByEmail(principal.getName())
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        String createdBy = (currentUser.getName() != null && !currentUser.getName().isBlank())
                                        ? currentUser.getName()
                                        : currentUser.getEmail();

                        SupplyChainLog lastLog = supplyChainLogRepository
                                        .findTopByProductIdOrderByTimestampDesc(productId)
                                        .orElse(null);

                        if (currentUser.hasRole("ROLE_DISTRIBUTOR")) {

                                if (lastLog == null
                                                || (lastLog.getFromUserId() == null && lastLog.getToUserId() == null)
                                                || (lastLog.getToUserId() == null) // Initial upload might have null
                                                                                   // toUserId
                                // Or if the product is currently owned by a Farmer (we might need to check role
                                // of owner, but strictly:
                                // if toUserId is NOT me, and I am picking it up...
                                // Simplified: If I don't own it, and no other distributor owns it (checked
                                // below), I can pick it up.
                                ) {
                                        // Specific check: If it's already with another Distributor/Retailer, fail.
                                        // But here we are in the "First Pickup" block.
                                        // Let's refine: Allow pickup if:
                                        // 1. Log is null (naked product)
                                        // 2. Log exists but toUserId is NULL (initial state)
                                        // 3. Log exists, toUserId is NOT me, and toUserId is NOT another Distributor
                                        // (would be checked by conflict logic ideally, but for now we assume if it's
                                        // not confirmed by another D, we can take it).

                                        // Actually, the safest logic for "Pickup from Farmer" is:
                                        // If the last log is NOT confirmed by another Distributor.
                                        // But wait, if I pickup, I create a new log with toId = ME.

                                        // Let's just allow it if I am not already the owner.
                                        // And if it's not already "completed" or fully owned by someone else who isn't
                                        // a farmer.
                                        // For MVP, we presume if I see it in "Market", it's buyable.

                                        supplyChainService.addLog(
                                                        productId, null, currentUser.getId(), location,
                                                        notes.isBlank() ? "Distributor received from farmer" : notes,
                                                        createdBy);

                                        // NEW: Reduce farmer's product quantity on procurement
                                        com.farmchainX.farmchainX.model.Product product = productRepository
                                                        .findById(productId)
                                                        .orElseThrow(() -> new RuntimeException("Product not found"));

                                        if (product.getQuantity() != null && product.getQuantity() > 0) {
                                                product.setQuantity(0.0); // Mark as fully procured (or reduce by
                                                                          // specific amount)
                                                product.setCurrentStatus("PROCURED");
                                                productRepository.save(product);
                                        }

                                        return ResponseEntity
                                                        .ok(Map.of("message", "You have taken possession from farmer"));
                                }

                                if (lastLog.isConfirmed() && lastLog.getToUserId() != null &&
                                                !lastLog.getToUserId().equals(currentUser.getId())) {
                                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                                        .body(Map.of("error",
                                                                        "Another distributor already has possession of this product"));
                                }

                                if (lastLog.getToUserId() != null
                                                && !lastLog.getToUserId().equals(currentUser.getId())) {
                                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                                        .body(Map.of("error",
                                                                        "You no longer have possession of this product"));
                                }

                                if (lastLog.getToUserId() == null || !lastLog.getToUserId().equals(currentUser.getId())
                                                || !lastLog.isConfirmed()) {
                                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                                        .body(Map.of("error",
                                                                        "You do not have confirmed possession of this product"));
                                }

                                if (toUserId != null) {
                                        if (toUserId.equals(currentUser.getId())) {
                                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                                .body(Map.of("error", "Cannot handover to yourself"));
                                        }
                                        supplyChainService.addLog(
                                                        productId, currentUser.getId(), toUserId, location,
                                                        notes.isBlank() ? "Final handover to retailer" : notes,
                                                        createdBy);
                                        return ResponseEntity.ok(Map.of("message", "Product handed over to retailer"));
                                }

                                supplyChainService.addLog(
                                                productId, currentUser.getId(), currentUser.getId(), location,
                                                notes.isBlank() ? "In-transit update" : notes,
                                                createdBy);
                                return ResponseEntity.ok(Map.of("message", "Supply chain updated"));

                        } else if (currentUser.hasRole("ROLE_RETAILER")) {

                                if (lastLog == null || !lastLog.getToUserId().equals(currentUser.getId())
                                                || lastLog.getFromUserId() == null || lastLog.isConfirmed()) {
                                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                                        .body(Map.of("error",
                                                                        "No pending handover for you to confirm"));
                                }

                                supplyChainService.confirmReceipt(productId, currentUser.getId(), location, notes,
                                                createdBy);

                                // NEW: Create or update RetailerInventory when confirming receipt
                                try {
                                        com.farmchainX.farmchainX.model.Product product = productRepository
                                                        .findById(productId)
                                                        .orElse(null);

                                        if (product != null) {
                                                // Check if inventory record already exists
                                                java.util.Optional<com.farmchainX.farmchainX.model.RetailerInventory> existingInventory = retailerInventoryRepository
                                                                .findByRetailerIdAndProductId(
                                                                                currentUser.getId(), productId);

                                                com.farmchainX.farmchainX.model.RetailerInventory inventory;

                                                if (existingInventory.isPresent()) {
                                                        // Update existing inventory
                                                        inventory = existingInventory.get();
                                                        Double currentQty = inventory.getQuantity() != null
                                                                        ? inventory.getQuantity()
                                                                        : 0.0;
                                                        inventory.setQuantity(currentQty + 100.0); // Add 100kg
                                                                                                   // (default)
                                                } else {
                                                        // Create new inventory record
                                                        inventory = new com.farmchainX.farmchainX.model.RetailerInventory();
                                                        inventory.setRetailerId(currentUser.getId());
                                                        inventory.setProductId(productId);
                                                        inventory.setQuantity(100.0); // Default 100kg
                                                        inventory.setSourceDistributorId(lastLog.getFromUserId());
                                                        inventory.setReceivedDate(java.time.LocalDateTime.now());
                                                }

                                                // Set price with markup
                                                Double basePrice = product.getPrice() != null ? product.getPrice()
                                                                : 100.0;
                                                inventory.setPricePerUnit(basePrice * 1.25); // 25% markup
                                                inventory.setStatus("IN_STOCK");
                                                inventory.setLastUpdated(java.time.LocalDateTime.now());

                                                retailerInventoryRepository.save(inventory);
                                                System.out.println(
                                                                "[Retailer Confirmation] Created/updated inventory for product "
                                                                                +
                                                                                productId + " for retailer "
                                                                                + currentUser.getId());
                                        }
                                } catch (Exception e) {
                                        // Log error but don't fail the confirmation
                                        System.err.println("[Retailer Confirmation] Error creating inventory: "
                                                        + e.getMessage());
                                }

                                return ResponseEntity.ok(Map.of("message", "Receipt confirmed successfully"));
                        }

                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Unauthorized"));
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", String.valueOf(e.getMessage())));
                }
        }

        @PreAuthorize("hasAnyRole('ADMIN','DISTRIBUTOR','RETAILER')")
        @GetMapping("/{productId}")
        public ResponseEntity<List<SupplyChainLog>> getProductChain(@PathVariable Long productId) {
                return ResponseEntity.ok(supplyChainService.getLogsByProduct(productId));
        }

        @PreAuthorize("hasRole('RETAILER')")
        @GetMapping("/pending")
        public ResponseEntity<?> getPendingForRetailer(
                        Principal principal,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "timestamp,desc") String sort) {

                User user = userRepository.findByEmail(principal.getName())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String[] parts = sort.split(",", 2);
                String sortProp = parts[0];
                boolean asc = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]);
                var pageable = org.springframework.data.domain.PageRequest.of(
                                page, size,
                                asc ? org.springframework.data.domain.Sort.Direction.ASC
                                                : org.springframework.data.domain.Sort.Direction.DESC,
                                sortProp);

                var pageRes = supplyChainLogRepository.findPendingForRetailer(user.getId(), pageable);
                return ResponseEntity.ok(pageRes);
        }

        @GetMapping("/inventory")
        @PreAuthorize("hasRole('DISTRIBUTOR')")
        public List<Map<String, Object>> getDistributorInventory(Principal principal) {
                User user = userRepository.findByEmail(principal.getName())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Logic: Find products where the LATEST log entry has this user as the
                // 'toUserId'
                // This is a bit complex in JPA, so we might iterate or use a custom query.
                // For MVP/Demo: Fetch all logs where toUser is this user, then group by
                // Product, pick latest.

                List<SupplyChainLog> allMyLogs = supplyChainLogRepository
                                .findByToUserIdOrderByTimestampDesc(user.getId());
                System.out.println("DEBUG: Distributor " + user.getId() + " has " + allMyLogs.size() + " logs.");

                // Group by productID
                Map<Long, SupplyChainLog> latestByProduct = new java.util.HashMap<>();
                for (SupplyChainLog log : allMyLogs) {
                        if (!latestByProduct.containsKey(log.getProductId())) {
                                latestByProduct.put(log.getProductId(), log);
                        }
                }

                System.out.println("DEBUG: Distinct products touching distributor: " + latestByProduct.size());

                List<Map<String, Object>> inventory = new java.util.ArrayList<>();

                for (SupplyChainLog log : latestByProduct.values()) {
                        System.out.println("DEBUG: Checking Product " + log.getProductId());
                        SupplyChainLog globalLatest = supplyChainLogRepository
                                        .findFirstByProductIdOrderByTimestampDesc(log.getProductId()).orElse(null);

                        if (globalLatest != null) {
                                System.out.println("DEBUG: Global Latest for " + log.getProductId() + ": ToUser="
                                                + globalLatest.getToUserId() + ", Confirmed="
                                                + globalLatest.isConfirmed());
                        }

                        if (globalLatest != null && globalLatest.getToUserId() != null
                                        && globalLatest.getToUserId().equals(user.getId())) {
                                // I am the current holder!
                                Map<String, Object> item = new java.util.HashMap<>();
                                item.put("productId", log.getProductId());
                                item.put("timestamp", log.getTimestamp());
                                item.put("location", log.getLocation());
                                System.out.println("DEBUG: Adding Product " + log.getProductId() + " to inventory.");

                                // Fetch product details
                                try {
                                        com.farmchainX.farmchainX.model.Product p = productService
                                                        .getProductById(log.getProductId());
                                        if (p != null) {
                                                item.put("cropName", p.getCropName());
                                                item.put("qualityGrade", p.getQualityGrade());
                                                item.put("quantity", "1000"); // Mock quantity
                                                item.put("unit", "kg");
                                                item.put("status", "In Stock");
                                                item.put("value", p.getPrice() != null ? p.getPrice() : 0.0);
                                                item.put("imagePath", p.getImagePath());
                                        }
                                } catch (Exception e) {
                                        // Ignore if product not found
                                }

                                inventory.add(item);
                        }
                }

                return inventory;
        }

        @GetMapping("/users/retailers")
        @PreAuthorize("hasRole('DISTRIBUTOR')")
        public List<Map<String, Serializable>> getRetailers() {
                return userRepository.findAll().stream()
                                .filter(user -> user.getRoles().stream()
                                                .anyMatch(role -> "ROLE_RETAILER".equals(role.getName())))
                                .map(user -> Map.<String, Serializable>of(
                                                "id", user.getId(),
                                                "name", user.getName(),
                                                "email", user.getEmail()))
                                .sorted((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")))
                                .toList();
        }

        @PostMapping("/broadcast-dispatch")
        @PreAuthorize("hasRole('DISTRIBUTOR')")
        @Transactional
        public ResponseEntity<?> broadcastDispatch(@RequestBody Map<String, Object> payload, Principal principal) {
                try {
                        Long productId = Long.valueOf(String.valueOf(payload.get("productId")));
                        String location = String.valueOf(payload.getOrDefault("location", "Distributor Warehouse"));
                        String notes = payload.containsKey("notes") && payload.get("notes") != null
                                        ? String.valueOf(payload.get("notes"))
                                        : "Product dispatched to all retailers";

                        User distributor = userRepository.findByEmail(principal.getName())
                                        .orElseThrow(() -> new RuntimeException("Distributor not found"));

                        // Verify distributor owns the product
                        SupplyChainLog lastLog = supplyChainLogRepository
                                        .findTopByProductIdOrderByTimestampDesc(productId)
                                        .orElse(null);

                        if (lastLog == null || !lastLog.getToUserId().equals(distributor.getId())
                                        || !lastLog.isConfirmed()) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("error",
                                                                "You do not have confirmed possession of this product"));
                        }

                        // Check if there's already a pending offer for this product
                        if (dispatchOfferRepository.existsByProductIdAndStatus(productId, "PENDING")) {
                                return ResponseEntity.status(HttpStatus.CONFLICT)
                                                .body(Map.of("error",
                                                                "A pending dispatch offer already exists for this product"));
                        }

                        // Create the dispatch offer
                        DispatchOffer offer = new DispatchOffer();
                        offer.setProductId(productId);
                        offer.setDistributorId(distributor.getId());
                        offer.setLocation(location);
                        offer.setNotes(notes);
                        offer.setStatus("PENDING");

                        dispatchOfferRepository.save(offer);

                        return ResponseEntity.ok(Map.of(
                                        "message", "Product dispatched to all retailers successfully",
                                        "offerId", offer.getId()));

                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", e.getMessage()));
                }
        }

        @PreAuthorize("hasRole('DISTRIBUTOR')")
        @GetMapping("/dashboard/distributor")
        public ResponseEntity<?> getDistributorStats(Principal principal) {
                User distributor = userRepository.findByEmail(principal.getName())
                                .orElseThrow(() -> new RuntimeException("Distributor not found"));

                // 1. Get Inventory (Active Batches)
                List<Map<String, Object>> inventory = getDistributorInventory(principal);
                int activeBatches = inventory.size();
                double totalValue = inventory.stream()
                                .mapToDouble(i -> (Double) i.getOrDefault("value", 0.0))
                                .sum();

                // 2. Get Recent Activities
                List<SupplyChainLog> myLogs = supplyChainLogRepository
                                .findByToUserIdOrderByTimestampDesc(distributor.getId());

                long connectedFarmers = inventory.stream()
                                .map(i -> {
                                        try {
                                                com.farmchainX.farmchainX.model.Product p = productService
                                                                .getProductById((Long) i.get("productId"));
                                                return p.getFarmer() != null ? p.getFarmer().getId() : -1L;
                                        } catch (Exception e) {
                                                return -1L;
                                        }
                                })
                                .filter(id -> id != -1L)
                                .distinct()
                                .count();

                List<Map<String, Object>> activities = myLogs.stream()
                                .limit(5)
                                .map(log -> {
                                        Map<String, Object> act = new java.util.HashMap<>();
                                        String type = "logistics";
                                        String text = log.getNotes();
                                        if (text == null)
                                                text = "Update";

                                        if (text.contains("Distributor received"))
                                                type = "purchase";
                                        if (text.contains("Handover"))
                                                type = "alert";

                                        act.put("type", type);
                                        act.put("text", text);
                                        act.put("time", log.getTimestamp().toString());
                                        return act;
                                })
                                .collect(java.util.stream.Collectors.toList());

                // 3. Chart Data
                // Inventory Distribution
                Map<String, Long> cropDistribution = inventory.stream()
                                .collect(java.util.stream.Collectors.groupingBy(
                                                i -> (String) i.getOrDefault("cropName", "Unknown"),
                                                java.util.stream.Collectors.counting()));

                // Sales History (Last 6 months) - Sales defined as Handover to Retailer (From
                // Me -> Not Me)
                List<SupplyChainLog> salesLogs = supplyChainLogRepository.findByFromUserId(distributor.getId()).stream()
                                .filter(l -> l.getToUserId() != null && !l.getToUserId().equals(distributor.getId()))
                                .collect(java.util.stream.Collectors.toList());

                Map<String, Double> salesHistory = new java.util.LinkedHashMap<>();
                java.time.YearMonth current = java.time.YearMonth.now();

                // Pre-fetch sales products to avoid N+1 inside loop if possible, or just accept
                // it for now.
                // We'll iterate months.
                for (int i = 5; i >= 0; i--) {
                        java.time.YearMonth target = current.minusMonths(i);
                        String key = target.getMonth().toString().substring(0, 3);
                        double val = salesLogs.stream()
                                        .filter(l -> java.time.YearMonth.from(l.getTimestamp()).equals(target))
                                        .mapToDouble(l -> {
                                                try {
                                                        com.farmchainX.farmchainX.model.Product p = productService
                                                                        .getProductById(l.getProductId());
                                                        return p.getPrice() != null ? p.getPrice() * 1.25 : 0; // Approx
                                                                                                               // Distributor
                                                                                                               // Sales
                                                                                                               // Price
                                                } catch (Exception e) {
                                                        return 0.0;
                                                }
                                        }).sum();
                        salesHistory.put(key, val);
                }

                Map<String, Object> stats = new java.util.HashMap<>();
                stats.put("activeBatches", activeBatches);
                stats.put("totalValue", totalValue);
                stats.put("connectedFarmers", connectedFarmers);
                long pendingOrders = supplyChainLogRepository.countPendingHandover(distributor.getId());
                stats.put("pendingOrders", pendingOrders);
                stats.put("recentActivities", activities);
                stats.put("chartData", Map.of("inventory", cropDistribution, "sales", salesHistory));

                return ResponseEntity.ok(stats);
        }

        @PreAuthorize("hasRole('DISTRIBUTOR')")
        @GetMapping("/dispatch-history")
        public List<Map<String, Object>> getDispatchHistory(Principal principal) {
                User distributor = userRepository.findByEmail(principal.getName())
                                .orElseThrow(() -> new RuntimeException("Distributor not found"));

                // Get all logs where this distributor dispatched products to retailers
                // (fromUser = distributor, toUser = retailer)
                List<SupplyChainLog> dispatchLogs = supplyChainLogRepository.findByFromUserId(distributor.getId())
                                .stream()
                                .filter(l -> l.getToUserId() != null && !l.getToUserId().equals(distributor.getId()))
                                .collect(java.util.stream.Collectors.toList());

                List<Map<String, Object>> history = new java.util.ArrayList<>();

                for (SupplyChainLog log : dispatchLogs) {
                        Map<String, Object> item = new java.util.HashMap<>();
                        item.put("id", log.getId());
                        item.put("productId", log.getProductId());
                        item.put("timestamp", log.getTimestamp());
                        item.put("createdAt", log.getTimestamp());
                        item.put("location", log.getLocation());
                        item.put("notes", log.getNotes());
                        item.put("confirmed", log.isConfirmed());

                        // Fetch product details
                        try {
                                com.farmchainX.farmchainX.model.Product p = productService
                                                .getProductById(log.getProductId());
                                if (p != null) {
                                        item.put("cropName", p.getCropName());
                                        item.put("value", p.getPrice() != null ? p.getPrice() : 0.0);
                                        item.put("imagePath", p.getImagePath());
                                }
                        } catch (Exception e) {
                                // Ignore if product not found
                        }

                        // Fetch retailer details
                        try {
                                User retailer = userRepository.findById(log.getToUserId()).orElse(null);
                                if (retailer != null) {
                                        item.put("retailerName", retailer.getName());
                                        item.put("retailerEmail", retailer.getEmail());
                                }
                        } catch (Exception e) {
                                // Ignore if retailer not found
                        }

                        history.add(item);
                }

                return history;
        }

        @PreAuthorize("hasRole('RETAILER')")
        @GetMapping("/retailer/inventory")
        public List<Map<String, Object>> getRetailerInventory(Principal principal) {
                User user = userRepository.findByEmail(principal.getName())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Logic: Find products where I am the 'toUser', it IS confirmed,
                // AND there is no newer log where I am 'fromUser' (meaning I haven't sold it
                // yet).

                List<SupplyChainLog> myReceipts = supplyChainLogRepository
                                .findByToUserIdOrderByTimestampDesc(user.getId());

                Map<Long, SupplyChainLog> latestReceiptByProduct = new java.util.HashMap<>();
                for (SupplyChainLog log : myReceipts) {
                        if (log.isConfirmed() && !latestReceiptByProduct.containsKey(log.getProductId())) {
                                latestReceiptByProduct.put(log.getProductId(), log);
                        }
                }

                List<Map<String, Object>> inventory = new java.util.ArrayList<>();

                for (SupplyChainLog receipt : latestReceiptByProduct.values()) {
                        // Check if I still have it (no newer log from me)
                        // Simplified: Check global latest
                        SupplyChainLog globalLatest = supplyChainLogRepository
                                        .findFirstByProductIdOrderByTimestampDesc(receipt.getProductId()).orElse(null);

                        if (globalLatest != null && globalLatest.getToUserId() != null
                                        && globalLatest.getToUserId().equals(user.getId())
                                        && globalLatest.isConfirmed()) {

                                // I am the current confirmed holder
                                Map<String, Object> item = new java.util.HashMap<>();
                                item.put("productId", String.valueOf(receipt.getProductId()));
                                item.put("name", "Unknown Crop"); // Defaults
                                // Fetch product details
                                try {
                                        com.farmchainX.farmchainX.model.Product p = productService
                                                        .getProductById(receipt.getProductId());
                                        if (p != null) {
                                                item.put("name", p.getCropName());
                                                item.put("batchId", String.valueOf(p.getId()));
                                                item.put("qtyOnHand", 500); // Mock
                                                item.put("unit", "kg");
                                                item.put("costPrice", p.getPrice());
                                                item.put("sellPrice", p.getPrice() * 1.25); // Mock markup
                                                item.put("supplier", "Distributor"); // Can be fetched from log.fromUser
                                                item.put("imagePath", p.getImagePath());
                                        }
                                } catch (Exception e) {
                                }

                                inventory.add(item);
                        }
                }
                return inventory;
        }

        @PreAuthorize("hasRole('RETAILER')")
        @GetMapping("/retailer/dashboard-stats")
        public ResponseEntity<?> getRetailerStats(Principal principal) {
                // 1. Inventory Stats
                List<Map<String, Object>> inv = getRetailerInventory(principal);
                double totalValue = inv.stream().mapToDouble(i -> (Double) i.getOrDefault("costPrice", 0.0)).sum();

                // 2. Pending Shipments (Incoming)
                // reuse getPendingForRetailer logic or call repository directly
                // simplified:
                User user = userRepository.findByEmail(principal.getName()).orElseThrow();
                // Just count pending logs
                // This requires a new repo method or reusing existing one.
                // For now, let's mock the count or assume 0 if query is complex,
                // but we can call our own getPendingForRetailer controller method logic
                // technically
                // but that returns ResponseEntity.
                // Let's just return simple stats.

                Map<String, Object> stats = new java.util.HashMap<>();
                stats.put("inventoryValue", totalValue);
                stats.put("openPOs", 5); // Mock
                stats.put("incomingShipments", 2); // Mock or fetch real count
                stats.put("lowStock", 0);

                return ResponseEntity.ok(stats);
        }

        @GetMapping("/market/distributors")
        public List<Map<String, Object>> getDistributorMarket() {
                // Fetch all users with ROLE_DISTRIBUTOR
                List<User> distributors = userRepository.findAll().stream()
                                .filter(u -> u.getRoles().stream()
                                                .anyMatch(r -> "ROLE_DISTRIBUTOR".equals(r.getName())))
                                .collect(java.util.stream.Collectors.toList());

                List<Map<String, Object>> marketItems = new java.util.ArrayList<>();

                for (User distributor : distributors) {
                        // For each distributor, fetch their current inventory
                        List<SupplyChainLog> logs = supplyChainLogRepository
                                        .findByToUserIdOrderByTimestampDesc(distributor.getId());

                        // Deduplicate by Product ID to find latest status
                        Map<Long, SupplyChainLog> latestByProduct = new java.util.HashMap<>();
                        for (SupplyChainLog log : logs) {
                                if (!latestByProduct.containsKey(log.getProductId())) {
                                        latestByProduct.put(log.getProductId(), log);
                                }
                        }

                        for (SupplyChainLog log : latestByProduct.values()) {
                                // Check if they still have it (global check)
                                SupplyChainLog globalLatest = supplyChainLogRepository
                                                .findFirstByProductIdOrderByTimestampDesc(log.getProductId())
                                                .orElse(null);

                                if (globalLatest != null && globalLatest.getToUserId() != null
                                                && globalLatest.getToUserId().equals(distributor.getId())) {

                                        // Valid item in distributor inventory
                                        Map<String, Object> item = new java.util.HashMap<>();
                                        item.put("id", log.getId()); // Use Log ID as item ID just for reference, or
                                                                     // mocked unique listing ID
                                        item.put("distributorId", distributor.getId());
                                        item.put("distributor", distributor.getName());
                                        item.put("location", "Retail Hub");
                                        item.put("verified", true);

                                        // Product Details
                                        // Let's wrap in try-catch or assume safe if we trust data integrity
                                        try {
                                                com.farmchainX.farmchainX.model.Product p = productService
                                                                .getProductById(log.getProductId());
                                                if (p != null) {
                                                        item.put("productId", p.getId());
                                                        item.put("cropName", p.getCropName());
                                                        item.put("quantity", 100); // Mock per item
                                                        item.put("unit", "kg");
                                                        item.put("pricePerUnit", p.getPrice());
                                                        item.put("quality", p.getQualityGrade());
                                                }
                                        } catch (Exception e) {
                                        }

                                        marketItems.add(item);
                                }
                        }
                }
                return marketItems;
        }

        @PostMapping("/purchase")
        @PreAuthorize("hasAnyRole('CONSUMER')")
        @Transactional
        public ResponseEntity<?> purchaseProduct(@RequestBody Map<String, Object> payload, Principal principal) {
                try {
                        Long productId = Long.valueOf(String.valueOf(payload.get("productId")));
                        String location = (String) payload.getOrDefault("location", "Online Store");

                        User consumer = userRepository.findByEmail(principal.getName())
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        SupplyChainLog lastLog = supplyChainLogRepository
                                        .findTopByProductIdOrderByTimestampDesc(productId)
                                        .orElse(null);

                        // Determine the source of the product (farmer or retailer)
                        Long fromUserId = lastLog != null ? lastLog.getToUserId() : null;
                        boolean isFromRetailer = false;

                        // Check if this product is in retailer inventory
                        if (fromUserId != null) {
                                User fromUser = userRepository.findById(fromUserId).orElse(null);
                                if (fromUser != null) {
                                        isFromRetailer = fromUser.getRoles().stream()
                                                        .anyMatch(r -> "ROLE_RETAILER".equals(r.getName()));
                                }
                        }

                        // Check product availability based on source
                        if (isFromRetailer && fromUserId != null) {
                                // For retailer products, check RetailerInventory
                                java.util.Optional<com.farmchainX.farmchainX.model.RetailerInventory> inventoryOpt = retailerInventoryRepository
                                                .findByRetailerIdAndProductId(fromUserId, productId);

                                if (!inventoryOpt.isPresent()) {
                                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                                        .body(Map.of("error",
                                                                        "Product not available in retailer inventory"));
                                }

                                com.farmchainX.farmchainX.model.RetailerInventory inventory = inventoryOpt.get();
                                if (inventory.getQuantity() == null || inventory.getQuantity() <= 0) {
                                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                                        .body(Map.of("error", "Product is out of stock"));
                                }
                        } else {
                                // For farmer products, check supply chain log
                                if (lastLog != null && lastLog.getNotes() != null
                                                && lastLog.getNotes().contains("Sold to Consumer")) {
                                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                                        .body(Map.of("error", "Product already sold"));
                                }
                        }

                        // Create Purchase Log
                        SupplyChainLog purchaseLog = new SupplyChainLog();
                        purchaseLog.setProductId(productId);
                        purchaseLog.setFromUserId(fromUserId);
                        purchaseLog.setToUserId(consumer.getId());
                        purchaseLog.setLocation(location);
                        purchaseLog.setNotes("Sold to Consumer " + consumer.getName() +
                                        (isFromRetailer ? " (via Retailer)" : " (Direct from Farmer)"));
                        purchaseLog.setCreatedBy(consumer.getEmail());
                        purchaseLog.setConfirmed(true);
                        purchaseLog.setConfirmedAt(java.time.LocalDateTime.now());
                        purchaseLog.setConfirmedById(consumer.getId());
                        purchaseLog.setTimestamp(java.time.LocalDateTime.now());

                        String prevHash = lastLog != null ? lastLog.getHash() : "";
                        purchaseLog.setPrevHash(prevHash);
                        purchaseLog.setHash(com.farmchainX.farmchainX.util.HashUtil.computeHash(purchaseLog, prevHash));

                        supplyChainLogRepository.save(purchaseLog);

                        // OPTIONAL: Reduce RetailerInventory quantity if purchasing from retailer
                        // This is non-blocking - if inventory record doesn't exist, purchase still
                        // succeeds
                        if (isFromRetailer && fromUserId != null) {
                                try {
                                        java.util.Optional<com.farmchainX.farmchainX.model.RetailerInventory> inventoryOpt = retailerInventoryRepository
                                                        .findByRetailerIdAndProductId(fromUserId, productId);

                                        if (inventoryOpt.isPresent()) {
                                                com.farmchainX.farmchainX.model.RetailerInventory inventory = inventoryOpt
                                                                .get();
                                                Double currentQty = inventory.getQuantity();

                                                if (currentQty != null && currentQty > 0) {
                                                        // Reduce quantity by 1 unit (or specific purchase quantity)
                                                        inventory.setQuantity(currentQty - 1.0);

                                                        // Update status if quantity reaches 0
                                                        if (inventory.getQuantity() <= 0) {
                                                                inventory.setStatus("OUT_OF_STOCK");
                                                        }

                                                        retailerInventoryRepository.save(inventory);
                                                        System.out.println(
                                                                        "[Purchase] Reduced retailer inventory for product "
                                                                                        + productId);
                                                }
                                        } else {
                                                // Inventory record doesn't exist - log but don't fail the purchase
                                                System.out.println("[Purchase] No inventory record found for retailer "
                                                                + fromUserId +
                                                                " and product " + productId
                                                                + " - purchase still successful");
                                        }
                                } catch (Exception e) {
                                        // Log error but don't fail the purchase
                                        System.err.println("[Purchase] Error updating inventory: " + e.getMessage());
                                }
                        }

                        return ResponseEntity.ok(Map.of(
                                        "message", "Purchase successful",
                                        "source", isFromRetailer ? "retailer" : "farmer"));
                } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "Purchase failed: " + e.getMessage()));
                }
        }

        @GetMapping("/consumer/history")
        @PreAuthorize("hasAnyRole('CONSUMER')")
        public List<Map<String, Object>> getConsumerHistory(Principal principal) {
                User consumer = userRepository.findByEmail(principal.getName())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<SupplyChainLog> myPurchases = supplyChainLogRepository
                                .findByToUserIdOrderByTimestampDesc(consumer.getId());

                List<Map<String, Object>> history = new java.util.ArrayList<>();

                for (SupplyChainLog log : myPurchases) {
                        // Basic check: is this a purchase?
                        if (log.getNotes() != null && log.getNotes().contains("Sold to Consumer")) {
                                Map<String, Object> item = new java.util.HashMap<>();
                                item.put("id", "ORD-" + log.getId()); // Mock Order ID
                                item.put("date", log.getTimestamp());
                                item.put("total", 0); // Placeholder
                                item.put("status", "Delivered");
                                item.put("vendor", "FarmChainX Market");

                                // Fetch Product Details
                                try {
                                        com.farmchainX.farmchainX.model.Product p = productService
                                                        .getProductById(log.getProductId());
                                        if (p != null) {
                                                item.put("total", p.getPrice());
                                                item.put("items", List.of(Map.of(
                                                                "name", p.getCropName(),
                                                                "qty", "1 unit",
                                                                "price", p.getPrice(),
                                                                "image", p.getImagePath())));
                                                item.put("ecoScore", 90 + (p.getId() % 10)); // Mock score
                                                item.put("productId", p.getId());
                                                item.put("publicUuid", p.getPublicUuid());
                                        }
                                } catch (Exception e) {
                                        item.put("items", List.of());
                                }
                                history.add(item);
                        }
                }
                return history;
        }

        private void logDebug(String msg) {
                try {
                        // Use a safe temp path or project root
                        Path path = Path.of("debug_chain_log.txt");
                        if (!Files.exists(path))
                                Files.createFile(path);
                        String line = java.time.LocalDateTime.now() + ": " + msg + "\n";
                        Files.writeString(path, line, StandardOpenOption.APPEND);
                } catch (Exception e) {
                        // Squelch
                }
        }
}
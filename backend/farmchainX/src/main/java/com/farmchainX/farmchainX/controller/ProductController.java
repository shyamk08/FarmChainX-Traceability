package com.farmchainX.farmchainX.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.farmchainX.farmchainX.model.AIPrediction;
import com.farmchainX.farmchainX.model.Product;
import com.farmchainX.farmchainX.model.SupplyChainLog;
import com.farmchainX.farmchainX.model.User;
import com.farmchainX.farmchainX.model.RetailerInventory;
import com.farmchainX.farmchainX.repository.AIPredictionRepository;
import com.farmchainX.farmchainX.repository.FeedbackRepository;
import com.farmchainX.farmchainX.repository.ProductRepository;
import com.farmchainX.farmchainX.repository.SupplyChainLogRepository;
import com.farmchainX.farmchainX.repository.UserRepository;
import com.farmchainX.farmchainX.repository.RetailerInventoryRepository;
import com.farmchainX.farmchainX.service.ProductService;
import com.farmchainX.farmchainX.util.HashUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final SupplyChainLogRepository supplyChainLogRepository;
    private final FeedbackRepository feedbackRepository;
    private final com.farmchainX.farmchainX.service.GroqAIService groqAIService;
    private final AIPredictionRepository aiPredictionRepository;
    private final RetailerInventoryRepository retailerInventoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductController(ProductService productService,
            UserRepository userRepository,
            ProductRepository productRepository,
            SupplyChainLogRepository supplyChainLogRepository,
            FeedbackRepository feedbackRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.farmchainX.farmchainX.service.GroqAIService groqAIService,
            AIPredictionRepository aiPredictionRepository,
            RetailerInventoryRepository retailerInventoryRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.supplyChainLogRepository = supplyChainLogRepository;
        this.feedbackRepository = feedbackRepository;
        this.groqAIService = groqAIService;
        this.aiPredictionRepository = aiPredictionRepository;
        this.retailerInventoryRepository = retailerInventoryRepository;
    }

    @PostMapping("/products/upload")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<?> uploadProduct(
            @RequestParam String cropName,
            @RequestParam String soilType,
            @RequestParam String pesticides,
            @RequestParam String harvestDate,
            @RequestParam String gpsLocation,
            @RequestParam(required = false, defaultValue = "0.0") Double price,
            @RequestParam(required = false, defaultValue = "1000.0") Double quantity,
            @RequestParam(required = false, defaultValue = "kg") String quantityUnit,
            @RequestParam("image") MultipartFile imageFile,
            Principal principal) throws IOException {

        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }
            if (imageFile == null || imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Image is required"));
            }

            String email = principal.getName();
            User farmer = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Farmer not found"));

            com.cloudinary.Cloudinary cloudinary = new com.cloudinary.Cloudinary(
                    "cloudinary://576328924368997:w91CTYnN6GDpASZhjff1oyeKIwk@dui3x1lur" // Please change the infos
            );
            java.util.Map uploadResult = cloudinary.uploader().upload(
                    imageFile.getBytes(),
                    com.cloudinary.utils.ObjectUtils.asMap("folder", "farmchainx/products"));
            String imagePath = uploadResult.get("secure_url").toString();

            LocalDate parsedDate = null;
            if (harvestDate != null && !harvestDate.isBlank()) {
                try {
                    parsedDate = LocalDate.parse(harvestDate, DateTimeFormatter.ISO_DATE);
                } catch (DateTimeParseException ex) {
                    String cleaned = harvestDate.replace(" ", "").replace("−", "-").replace("—", "-").replace("/", "-");
                    parsedDate = LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                }
            }

            Product product = new Product();
            product.setCropName(cropName);
            product.setSoilType(soilType);
            product.setPesticides(pesticides);
            product.setHarvestDate(parsedDate);
            product.setGpsLocation(gpsLocation);
            product.setImagePath(imagePath);
            product.setFarmer(farmer);
            product.setQualityGrade(null);
            product.setConfidenceScore(null);
            product.setPrice(price);
            product.setQuantity(quantity);
            product.setQuantityUnit(quantityUnit);

            // NEW: Auto-generate batch ID if not provided
            product.setBatchId("BATCH-" + System.currentTimeMillis());

            // NEW: Set initial status
            product.setCurrentStatus("CREATED");

            // NEW: Calculate expiry date (30 days from harvest for now, can be customized)
            if (parsedDate != null) {
                product.setExpiryDate(parsedDate.plusDays(30));
            }

            Product saved = productService.saveProduct(product);

            // Resolve Address once on upload
            String resolvedAddress = productService.resolveAddressFromGps(gpsLocation);
            saved.setAddress(resolvedAddress);

            saved.ensurePublicUuid();
            productRepository.save(saved);

            // Generate AI prediction using Groq (if service available)
            Map<String, Object> aiPrediction = new HashMap<>();
            if (groqAIService != null) {
                aiPrediction = groqAIService.generateFarmPrediction(saved);
            } else {
                // Fallback prediction when AI service is not available
                aiPrediction.put("qualityGrade", "B");
                aiPrediction.put("qualityScore", 75);
                aiPrediction.put("confidence", 85);
                aiPrediction.put("marketReadiness", "Ready for market");
                aiPrediction.put("storageRecommendation", "Store in cool, dry place. Maintain proper ventilation.");
                aiPrediction.put("optimalSellingWindow", "1-3 days after harvest");
                aiPrediction.put("priceEstimate", "Market price varies");
                aiPrediction.put("insights", Arrays.asList(
                        "Product appears to be in good condition",
                        "Monitor storage conditions regularly",
                        "Consider local market demand"));
                aiPrediction.put("warnings", new ArrayList<>());
                aiPrediction.put("certificationEligibility", "Conventional");
            }

            // Save AI prediction to database (only if prediction was generated)
            if (!aiPrediction.isEmpty()) {
                try {
                    AIPrediction prediction = new AIPrediction();
                    prediction.setProduct(saved);
                    prediction.setFarmer(farmer);

                    // Store full prediction as JSON
                    String predictionJson = objectMapper.writeValueAsString(aiPrediction);
                    prediction.setPredictionData(predictionJson);

                    // Extract and store key fields for easier querying
                    if (aiPrediction.containsKey("qualityGrade")) {
                        prediction.setQualityGrade(String.valueOf(aiPrediction.get("qualityGrade")));
                    }
                    if (aiPrediction.containsKey("qualityScore")) {
                        Object score = aiPrediction.get("qualityScore");
                        if (score instanceof Number) {
                            prediction.setQualityScore(((Number) score).intValue());
                        }
                    }
                    if (aiPrediction.containsKey("confidence")) {
                        Object conf = aiPrediction.get("confidence");
                        if (conf instanceof Number) {
                            prediction.setConfidence(((Number) conf).intValue());
                        }
                    }
                    if (aiPrediction.containsKey("marketReadiness")) {
                        prediction.setMarketReadiness(String.valueOf(aiPrediction.get("marketReadiness")));
                    }
                    if (aiPrediction.containsKey("storageRecommendation")) {
                        prediction.setStorageRecommendation(String.valueOf(aiPrediction.get("storageRecommendation")));
                    }
                    if (aiPrediction.containsKey("optimalSellingWindow")) {
                        prediction.setOptimalSellingWindow(String.valueOf(aiPrediction.get("optimalSellingWindow")));
                    }
                    if (aiPrediction.containsKey("priceEstimate")) {
                        prediction.setPriceEstimate(String.valueOf(aiPrediction.get("priceEstimate")));
                    }
                    if (aiPrediction.containsKey("certificationEligibility")) {
                        prediction
                                .setCertificationEligibility(
                                        String.valueOf(aiPrediction.get("certificationEligibility")));
                    }

                    aiPredictionRepository.save(prediction);
                } catch (Exception e) {
                    System.err.println("[AI Prediction Save Error] " + e.getMessage());
                    // Continue even if saving prediction fails
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", saved.getId());
            response.put("message", "Product uploaded successfully");
            response.put("qualityGrade", saved.getQualityGrade());
            response.put("confidenceScore", saved.getConfidenceScore());
            response.put("aiPrediction", aiPrediction);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/products/my")
    public ResponseEntity<?> getMyProducts(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        String email = principal.getName();
        User farmer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        String[] parts = sort.split(",", 2);
        String sortProp = parts[0];
        boolean asc = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]);

        Pageable pageable = PageRequest.of(
                page,
                size,
                asc ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortProp);

        org.springframework.data.domain.Page<Product> pageRes = productRepository.findByFarmerId(farmer.getId(),
                pageable);

        // Map to include status
        org.springframework.data.domain.Page<Map<String, Object>> dtoPage = pageRes.map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("cropName", p.getCropName());
            map.put("soilType", p.getSoilType());
            map.put("pesticides", p.getPesticides());
            map.put("harvestDate", p.getHarvestDate());
            map.put("gpsLocation", p.getGpsLocation());
            map.put("imagePath", p.getImagePath());
            map.put("qualityGrade", p.getQualityGrade());
            map.put("confidenceScore", p.getConfidenceScore());
            map.put("price", p.getPrice());
            map.put("address", p.getAddress());
            map.put("publicUuid", p.getPublicUuid());
            map.put("qrCodePath", p.getQrCodePath());

            boolean isSold = supplyChainLogRepository.existsByProductId(p.getId());
            map.put("status", isSold ? "Sold" : "Active");
            map.put("sold", isSold);
            return map;
        });

        return ResponseEntity.ok(dtoPage);
    }

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/predictions/my")
    public ResponseEntity<?> getMyPredictions(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        String email = principal.getName();
        User farmer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        List<AIPrediction> predictions = aiPredictionRepository.findByFarmerIdOrderByCreatedAtDesc(farmer.getId());

        // Convert to response format with product details
        List<Map<String, Object>> response = predictions.stream().map(prediction -> {
            Map<String, Object> predMap = new HashMap<>();
            predMap.put("id", prediction.getId());
            predMap.put("productId", prediction.getProduct().getId());
            predMap.put("productName", prediction.getProduct().getCropName());
            predMap.put("qualityGrade", prediction.getQualityGrade());
            predMap.put("qualityScore", prediction.getQualityScore());
            predMap.put("confidence", prediction.getConfidence());
            predMap.put("marketReadiness", prediction.getMarketReadiness());
            predMap.put("storageRecommendation", prediction.getStorageRecommendation());
            predMap.put("optimalSellingWindow", prediction.getOptimalSellingWindow());
            predMap.put("priceEstimate", prediction.getPriceEstimate());
            predMap.put("certificationEligibility", prediction.getCertificationEligibility());
            predMap.put("createdAt", prediction.getCreatedAt());

            // Parse full prediction data if available
            try {
                if (prediction.getPredictionData() != null) {
                    Map<String, Object> fullPrediction = objectMapper.readValue(
                            prediction.getPredictionData(),
                            Map.class);
                    predMap.put("fullPrediction", fullPrediction);
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }

            return predMap;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(Map.of("predictions", response, "total", response.size()));
    }

    @PreAuthorize("hasAnyRole('FARMER','ADMIN')")
    @PostMapping("/products/{id}/qrcode")
    public ResponseEntity<?> generateProductQrCode(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productService.getProductById(id);
        boolean isOwner = product.getFarmer().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()));

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "You can only generate QR for your own products"));
        }

        String qrPath = productService.generateProductQr(id);
        return ResponseEntity.ok(Map.of(
                "message", "QR Code generated successfully",
                "qrPath", qrPath,
                "verifyUrl", "https://yourdomain.com/verify/" + product.getPublicUuid()));
    }

    @GetMapping("/products/{id}/qrcode/download")
    public ResponseEntity<byte[]> downloadProductQR(@PathVariable Long id) {
        try {
            byte[] imageBytes = productService.getProductQRImage(id);
            return ResponseEntity.ok()
                    .header("Content-Type", "image/png")
                    .header("Content-Disposition", "attachment; filename=product_" + id + ".png")
                    .body(imageBytes);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(null);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','RETAILER','DISTRIBUTOR')")
    @GetMapping("/products/filter")
    public List<Product> filterProducts(
            @RequestParam(required = false) String cropName,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return productService.filterProducts(cropName, endDate);
    }

    @GetMapping("/products/{id}/public")
    public Map<String, Object> getPublicView(@PathVariable Long id) {
        return productService.getPublicView(id);
    }

    @PreAuthorize("hasAnyRole('FARMER','ADMIN','DISTRIBUTOR','RETAILER')")
    @GetMapping("/products/{id}/details")
    public Map<String, Object> getAuthorizedView(@PathVariable Long id, Principal principal) {
        return productService.getAuthorizedView(id, principal != null ? principal.getName() : null);
    }

    @GetMapping("/products/by-uuid/{uuid}/public")
    public ResponseEntity<?> getPublicByUuid(@PathVariable String uuid) {
        return productRepository.findByPublicUuid(uuid)
                .map(p -> ResponseEntity.ok(productService.getPublicView(p.getId())))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Product not found")));
    }

    @GetMapping("/verify/{uuid}")
    public ResponseEntity<?> verifyByUuid(@PathVariable("uuid") String uuid, Principal principal) {
        try {
            Map<String, Object> data = productService.getPublicViewByUuid(uuid);
            boolean canUpdate = false;
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName()).orElse(null);
                if (user != null) {
                    canUpdate = user.getRoles().stream().anyMatch(role -> "ROLE_DISTRIBUTOR".equals(role.getName())
                            || "ROLE_RETAILER".equals(role.getName()));
                }
            }
            data.put("canUpdate", canUpdate);

            boolean canGiveFeedback = false;
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName()).orElse(null);
                if (user != null) {
                    boolean isConsumer = user.getRoles().stream()
                            .anyMatch(r -> "ROLE_CONSUMER".equals(r.getName()));
                    if (isConsumer) {
                        Long productId = null;
                        if (data.containsKey("productId") && data.get("productId") instanceof Number) {
                            productId = ((Number) data.get("productId")).longValue();
                        } else {
                            productId = productRepository.findByPublicUuid(uuid)
                                    .map(p -> p.getId())
                                    .orElse(null);
                        }

                        if (productId != null) {
                            boolean already = feedbackRepository.findByProductIdAndConsumerId(productId, user.getId())
                                    .isPresent();
                            canGiveFeedback = !already;
                        }
                    }
                }
            }
            data.put("canGiveFeedback", canGiveFeedback);

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "Product not found"));
        }
    }

    @PostMapping("/verify/{uuid}/track")
    @PreAuthorize("hasAnyRole('RETAILER','DISTRIBUTOR')")
    public ResponseEntity<?> addTrackingByUuid(
            @PathVariable("uuid") String uuid,
            @RequestBody Map<String, Object> body,
            Principal principal) {

        String location = (String) body.get("location");
        String note = Optional.ofNullable((String) body.get("note")).orElse("").trim();
        Object toUserObj = body.get("toUserId");
        Long toUserId = (toUserObj instanceof Number) ? ((Number) toUserObj).longValue() : null;

        if (location == null || location.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Location is required"));
        }

        User currentUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Long productId = product.getId();
        SupplyChainLog lastLog = supplyChainLogRepository
                .findTopByProductIdOrderByTimestampDesc(productId)
                .orElse(null);

        if (currentUser.hasRole("ROLE_DISTRIBUTOR")) {

            if (lastLog == null || (lastLog.getToUserId() == null && lastLog.getFromUserId() == null)) {
                SupplyChainLog pickupLog = new SupplyChainLog();
                pickupLog.setProductId(productId);
                pickupLog.setFromUserId(null);
                pickupLog.setToUserId(currentUser.getId());
                pickupLog.setLocation(location);
                pickupLog.setNotes(note.isBlank() ? "Distributor collected from farmer" : note);
                pickupLog.setCreatedBy(currentUser.getEmail());
                pickupLog.setConfirmed(true);
                pickupLog.setTimestamp(LocalDateTime.now());

                String prevHash = lastLog != null ? lastLog.getHash() : "";
                pickupLog.setPrevHash(prevHash);
                pickupLog.setHash(HashUtil.computeHash(pickupLog, prevHash));

                supplyChainLogRepository.save(pickupLog);
                return ResponseEntity.ok(Map.of("message", "You have successfully taken the product from the farmer"));
            }

            if (lastLog.getToUserId() != null && lastLog.getToUserId().equals(currentUser.getId())
                    && toUserId == null) {
                SupplyChainLog trackingLog = new SupplyChainLog();
                trackingLog.setProductId(productId);
                trackingLog.setFromUserId(currentUser.getId());
                trackingLog.setToUserId(currentUser.getId());
                trackingLog.setLocation(location);
                trackingLog.setNotes(note.isBlank() ? "Tracking update by distributor" : note);
                trackingLog.setCreatedBy(currentUser.getEmail());
                trackingLog.setConfirmed(true);
                trackingLog.setTimestamp(LocalDateTime.now());
                trackingLog.setPrevHash(lastLog.getHash());
                trackingLog.setHash(HashUtil.computeHash(trackingLog, lastLog.getHash()));
                supplyChainLogRepository.save(trackingLog);

                return ResponseEntity.ok(Map.of("message", "Tracking updated"));
            }

            if (toUserId != null && lastLog.getToUserId() != null && lastLog.getToUserId().equals(currentUser.getId())
                    && lastLog.isConfirmed()) {
                if (!userRepository.existsById(toUserId)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Selected retailer does not exist"));
                }

                SupplyChainLog handover = new SupplyChainLog();
                handover.setProductId(productId);
                handover.setFromUserId(currentUser.getId());
                handover.setToUserId(toUserId);
                handover.setLocation(location);
                handover.setNotes(note.isBlank() ? "Handed over to retailer – awaiting confirmation" : note);
                handover.setCreatedBy(currentUser.getEmail());
                handover.setConfirmed(false);
                handover.setTimestamp(LocalDateTime.now());
                handover.setPrevHash(lastLog.getHash());
                handover.setHash(HashUtil.computeHash(handover, lastLog.getHash()));

                supplyChainLogRepository.save(handover);

                return ResponseEntity.ok(Map.of("message",
                        "Product successfully handed over! Only the selected retailer will see it in Pending Receipts."));
            }
        }

        if (currentUser.hasRole("ROLE_RETAILER")) {
            if (lastLog == null || !lastLog.getToUserId().equals(currentUser.getId()) || lastLog.isConfirmed()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No product pending for you to confirm"));
            }

            SupplyChainLog confirmLog = new SupplyChainLog();
            confirmLog.setProductId(productId);
            confirmLog.setFromUserId(lastLog.getFromUserId());
            confirmLog.setToUserId(currentUser.getId());
            confirmLog.setLocation(location);
            confirmLog.setNotes(note.isBlank() ? "Retailer confirmed receipt" : note);
            confirmLog.setCreatedBy(currentUser.getEmail());
            confirmLog.setConfirmed(true);
            confirmLog.setConfirmedAt(LocalDateTime.now());
            confirmLog.setConfirmedById(currentUser.getId());
            confirmLog.setTimestamp(LocalDateTime.now());
            confirmLog.setPrevHash(lastLog.getHash());
            confirmLog.setHash(HashUtil.computeHash(confirmLog, lastLog.getHash()));

            supplyChainLogRepository.save(confirmLog);

            return ResponseEntity.ok(Map.of("message", "Receipt confirmed successfully! Chain is now complete."));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Invalid action"));
    }

    @GetMapping("/products/market")
    public List<Map<String, Object>> getMarketProducts() {
        return productService.getMarketplaceProducts();
    }

    @GetMapping("/products/consumer")
    public List<Map<String, Object>> getConsumerProducts() {
        // Fetch products from retailer inventory only (IN_STOCK status)
        List<RetailerInventory> inventoryItems = retailerInventoryRepository.findByStatus("IN_STOCK");

        return inventoryItems.stream().map(inventory -> {
            Map<String, Object> item = new java.util.HashMap<>();

            // Get product details
            Product product = productRepository.findById(inventory.getProductId()).orElse(null);
            if (product == null) {
                return null;
            }

            // Get retailer details
            User retailer = userRepository.findById(inventory.getRetailerId()).orElse(null);

            // Product information
            item.put("id", product.getId());
            item.put("cropName", product.getCropName());
            item.put("qualityGrade", product.getQualityGrade());
            item.put("harvestDate", product.getHarvestDate());
            item.put("gpsLocation", product.getGpsLocation());
            item.put("displayLocation", product.getAddress());
            item.put("imagePath", product.getImagePath());
            item.put("publicUuid", product.getPublicUuid());

            // Retailer inventory specific info
            item.put("price", inventory.getPricePerUnit());
            item.put("quantity", inventory.getQuantity());
            item.put("inventoryId", inventory.getId());
            item.put("status", inventory.getStatus());

            // Retailer information
            if (retailer != null) {
                item.put("retailerId", retailer.getId());
                item.put("retailerName", retailer.getName());
                item.put("sellerName", retailer.getName());
            }

            // Farmer information
            if (product.getFarmer() != null) {
                item.put("farmerName", product.getFarmer().getName());
            }

            return item;
        })
                .filter(item -> item != null) // Filter out null items
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('CONSUMER', 'FARMER', 'DISTRIBUTOR', 'RETAILER', 'ADMIN')")
    public List<Map<String, Object>> getNotifications(Principal principal) {
        if (principal == null) {
            return List.of();
        }
        return productService.getUserNotifications(principal.getName());
    }
}

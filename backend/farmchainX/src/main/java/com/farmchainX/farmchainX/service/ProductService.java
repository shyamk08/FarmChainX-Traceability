package com.farmchainX.farmchainX.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.farmchainX.farmchainX.model.Product;
import com.farmchainX.farmchainX.model.SupplyChainLog;
import com.farmchainX.farmchainX.model.User;
import com.farmchainX.farmchainX.repository.ProductRepository;
import com.farmchainX.farmchainX.repository.SupplyChainLogRepository;
import com.farmchainX.farmchainX.repository.UserRepository;
import com.farmchainX.farmchainX.util.QrCodeGenerator;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final SupplyChainLogRepository supplyChainLogRepository;

    public ProductService(ProductRepository productRepository,
            UserRepository userRepository,
            AiService aiService,
            SupplyChainLogRepository supplyChainLogRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.supplyChainLogRepository = supplyChainLogRepository;
    }

    @Transactional
    public Product saveProduct(Product product) {
        Product saved = productRepository.save(product);
        try {
            String path = saved.getImagePath();
            if (path == null || path.isBlank()) {
                throw new IllegalStateException("Image path missing for grading");
            }

            // ✅ Let AiService handle URL or local file. Do NOT pre-check with File.exists()
            // here.
            Map<String, Object> aiResult = aiService.predictQuality(path);

            if (aiResult != null && aiResult.get("grade") != null && aiResult.get("confidence") != null) {
                saved.setQualityGrade(String.valueOf(aiResult.get("grade")));
                saved.setConfidenceScore(Double.parseDouble(aiResult.get("confidence").toString()));
                return productRepository.save(saved);
            } else {
                throw new IllegalStateException("AI returned empty result");
            }
        } catch (Exception e) {
            // Non-fatal: keep the product, leave grading as pending
            System.err.println("[AI Grading Error] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return saved;
        }
    }

    public List<Product> getProductsByFarmerId(Long farmerId) {
        userRepository.findById(farmerId).orElseThrow(() -> new RuntimeException("Farmer not found"));
        return productRepository.findByFarmerId(farmerId);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public List<Product> filterProducts(String cropName, LocalDate endDate) {
        return productRepository.filterProducts(cropName, endDate);
    }

    public String generateProductQr(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.ensurePublicUuid();
        productRepository.save(product);
        String publicUuid = product.getPublicUuid();

        String frontendBase = System.getenv("FRONTEND_URL");
        if (frontendBase == null || frontendBase.isBlank()) {
            frontendBase = "http://localhost:4200";
        }

        String qrText = frontendBase + "/verify/" + publicUuid;
        try {
            Path qrDir = Path.of("uploads", "qrcodes");
            Files.createDirectories(qrDir);
            String fileName = "qr_" + publicUuid + ".png";
            Path qrFilePath = qrDir.resolve(fileName);
            QrCodeGenerator.generateQR(qrText, qrFilePath.toString());
            String webPath = "/uploads/qrcodes/" + fileName;
            product.setQrCodePath(webPath);
            productRepository.save(product);
            return webPath;
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR code: " + e.getMessage(), e);
        }
    }

    public byte[] getProductQRImage(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        String qrPath = product.getQrCodePath();
        if (qrPath == null || qrPath.isEmpty())
            throw new RuntimeException("QR code not generated yet for this product");
        try {
            Path path1 = Path.of(qrPath.startsWith("/") ? qrPath.substring(1) : qrPath);
            Path path2 = Path.of("uploads", "qrcodes", "product_" + productId + ".png");
            Path actual = Files.exists(path1) ? path1 : (Files.exists(path2) ? path2 : null);
            if (actual == null)
                throw new RuntimeException("QR file not found.");
            return Files.readAllBytes(actual);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read the QR code image: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getPublicView(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        Map<String, Object> data = new HashMap<>();
        data.put("cropName", product.getCropName());
        data.put("harvestDate", product.getHarvestDate());
        data.put("qualityGrade", product.getQualityGrade() != null ? product.getQualityGrade() : "Pending");
        data.put("confidence", product.getConfidenceScore() != null ? product.getConfidenceScore() : 0.0);
        data.put("imagePath", product.getImagePath()); // Match frontend
        data.put("imageUrl", product.getImagePath());
        data.put("gpsLocation", product.getGpsLocation());
        data.put("displayLocation", resolveAddressFromGps(product.getGpsLocation()));
        data.put("originLocation", resolveAddressFromGps(product.getGpsLocation())); // Match frontend
        data.put("productId", product.getId());
        data.put("publicUuid", product.getPublicUuid());
        data.put("qrCodePath", product.getQrCodePath());
        data.put("price", product.getPrice());
        data.put("farmerName", product.getFarmer() != null ? product.getFarmer().getName() : "Unknown Farmer");

        List<SupplyChainLog> logs = supplyChainLogRepository.findByProductIdOrderByTimestampAsc(productId);
        List<Map<String, Object>> trackingHistory = logs.stream().map(log -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", log.getId());
            m.put("productId", log.getProductId());
            m.put("fromUserId", log.getFromUserId());
            m.put("toUserId", log.getToUserId());
            m.put("location",
                    log.getLocation() != null && !log.getLocation().isBlank() ? log.getLocation() : "Farm Origin");
            m.put("notes", log.getNotes() != null && !log.getNotes().isBlank() ? log.getNotes()
                    : "Product harvested and entered supply chain");
            m.put("timestamp", log.getTimestamp() != null ? log.getTimestamp() : LocalDateTime.now());
            m.put("createdBy",
                    log.getCreatedBy() != null && !log.getCreatedBy().isBlank() ? log.getCreatedBy() : "Farmer");
            m.put("prevHash", log.getPrevHash());
            m.put("hash", log.getHash());
            m.put("confirmed", log.isConfirmed());
            m.put("confirmedAt", log.getConfirmedAt());
            m.put("confirmedById", log.getConfirmedById());
            m.put("rejected", log.isRejected());
            m.put("rejectReason", log.getRejectReason());
            return m;
        }).collect(Collectors.toList());

        if (trackingHistory.isEmpty()) {
            Map<String, Object> initial = new HashMap<>();
            initial.put("id", null);
            initial.put("productId", product.getId());
            initial.put("fromUserId", null);
            initial.put("toUserId", null);
            initial.put("location", "Farm Origin");
            initial.put("notes", "Product harvested and entered the FarmChainX blockchain");
            initial.put("timestamp",
                    product.getHarvestDate() != null ? product.getHarvestDate().atStartOfDay() : LocalDateTime.now());
            initial.put("createdBy", "Farmer");
            initial.put("prevHash", "");
            initial.put("hash", "");
            initial.put("confirmed", false);
            initial.put("confirmedAt", null);
            initial.put("confirmedById", null);
            initial.put("rejected", false);
            initial.put("rejectReason", null);
            trackingHistory.add(initial);
        }

        data.put("trackingHistory", trackingHistory);
        data.put("logs", trackingHistory); // Match frontend
        return data;
    }

    public Map<String, Object> getPublicViewByUuid(String publicUuid) {
        Product product = productRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return getPublicView(product.getId());
    }

    public SupplyChainLog addTrackingByUuid(String publicUuid, String notes, String location, String addedByUsername) {
        Product product = productRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        SupplyChainLog log = new SupplyChainLog();
        log.setProductId(product.getId());
        log.setNotes(notes);
        log.setLocation(location);
        log.setTimestamp(LocalDateTime.now());
        log.setFromUserId(null);
        log.setToUserId(null);
        log.setCreatedBy(addedByUsername != null ? addedByUsername : "Anonymous User");
        return supplyChainLogRepository.save(log);
    }

    public Map<String, Object> getAuthorizedView(Long productId, Object userPrincipal) {
        Map<String, Object> data = new HashMap<>(getPublicView(productId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        data.put("soilType", product.getSoilType());
        data.put("pesticides", product.getPesticides());
        data.put("canUpdateChain", false);
        data.put("requestedBy", "unknown");
        try {
            if (userPrincipal instanceof UserDetails ud) {
                data.put("requestedBy", ud.getUsername());
            } else if (userPrincipal instanceof String s) {
                data.put("requestedBy", s);
            }
        } catch (Exception ignored) {
        }
        return data;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public String resolveAddressFromGps(String gps) {
        try {
            if (gps == null || !gps.contains(","))
                return gps;
            String[] parts = gps.split(",");
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());

            // Add a simple cache or delay if needed, but for now just call the API
            // Ideally, this is called ONLY on upload, so rate limits are less of an issue
            // than N+1 reads

            String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat="
                    + URLEncoder.encode(String.valueOf(lat), StandardCharsets.UTF_8)
                    + "&lon=" + URLEncoder.encode(String.valueOf(lon), StandardCharsets.UTF_8)
                    + "&zoom=10&addressdetails=1";

            RestTemplate rest = new RestTemplate();
            // User Agent is required by Nominatim
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("User-Agent", "FarmChainX/1.0");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("parameters",
                    headers);

            org.springframework.http.ResponseEntity<Map> response = rest.exchange(url,
                    org.springframework.http.HttpMethod.GET, entity, Map.class);

            if (response.getBody() == null)
                return gps;

            Object display = response.getBody().get("display_name");
            if (display == null)
                return gps;

            // Simplify the address to just City, State or similar if possible, but full
            // address is fine
            String fullAddress = display.toString();
            String[] tokens = fullAddress.split(",");
            if (tokens.length >= 2) {
                // Try to return something lie "City, State"
                return tokens[0].trim() + ", " + tokens[1].trim();
            }
            return fullAddress;
        } catch (Exception e) {
            System.err.println("Error resolving GPS: " + e.getMessage());
            return gps;
        }
    }

    public List<Map<String, Object>> getMarketplaceProducts() {
        return productRepository.findAll().stream()
                .map(product -> {
                    boolean isSold = supplyChainLogRepository.existsByProductId(product.getId());

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", product.getId());
                    map.put("cropName", product.getCropName());
                    map.put("price", product.getPrice());
                    map.put("imagePath", product.getImagePath());
                    map.put("harvestDate", product.getHarvestDate());
                    map.put("gpsLocation", product.getGpsLocation());
                    map.put("qualityGrade", product.getQualityGrade());
                    map.put("confidenceScore", product.getConfidenceScore());
                    map.put("farmerName",
                            product.getFarmer() != null ? product.getFarmer().getName() : "Unknown Farmer");
                    map.put("quantity", product.getQuantity() != null ? product.getQuantity() : 1000.0);
                    map.put("quantityUnit", product.getQuantityUnit() != null ? product.getQuantityUnit() : "kg");

                    if (product.getAddress() != null && !product.getAddress().isBlank()) {
                        map.put("displayLocation", product.getAddress());
                    } else {
                        map.put("displayLocation", product.getGpsLocation());
                    }

                    map.put("isSold", isSold);
                    map.put("status", isSold ? "Sold" : "Available");

                    return map;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getConsumerProducts() {
        // Fetch products that are currently with a Retailer (Confirmed)
        // Logic: specific logs where toUser has role RETAILER and is confirmed,
        // and no newer logs exist for that product.

        // Simplified: Iterate all products, check status.
        return productRepository.findAll().stream()
                .map(product -> {
                    SupplyChainLog lastLog = supplyChainLogRepository
                            .findTopByProductIdOrderByTimestampDesc(product.getId()).orElse(null);

                    if (lastLog != null && lastLog.isConfirmed() && lastLog.getToUserId() != null) {
                        User owner = userRepository.findById(lastLog.getToUserId()).orElse(null);
                        if (owner != null
                                && owner.getRoles().stream().anyMatch(r -> "ROLE_RETAILER".equals(r.getName()))) {

                            // Check if already sold to consumer
                            boolean soldToConsumer = lastLog.getNotes() != null &&
                                    lastLog.getNotes().contains("Sold to Consumer");

                            // Only return if NOT sold
                            if (!soldToConsumer) {
                                Map<String, Object> map = new HashMap<>();
                                map.put("id", product.getId());
                                map.put("cropName", product.getCropName());
                                // Retailer markup? For now use base price or mock markup
                                map.put("price", product.getPrice() != null ? product.getPrice() * 1.5 : 100);
                                map.put("imagePath", product.getImagePath());
                                map.put("harvestDate", product.getHarvestDate());
                                map.put("gpsLocation", product.getGpsLocation());
                                map.put("displayLocation", lastLog.getLocation()); // Retailer location
                                map.put("qualityGrade", product.getQualityGrade());
                                map.put("farmerName",
                                        product.getFarmer() != null ? product.getFarmer().getName() : "Unknown");
                                map.put("retailerName", owner.getName());
                                map.put("quantity", product.getQuantity() != null ? product.getQuantity() : 100.0);
                                map.put("quantityUnit",
                                        product.getQuantityUnit() != null ? product.getQuantityUnit() : "kg");
                                map.put("isSold", false);
                                map.put("status", "Available");
                                return map;
                            }
                        }
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getUserNotifications(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null)
            return List.of();

        // fetch logs where this user is the receiver or sender ?
        // For consumer: mostly receiver (bought items)
        // Let's get logs where toUserId == user.id
        List<SupplyChainLog> logs = supplyChainLogRepository.findAll().stream()
                .filter(l -> (l.getToUserId() != null && l.getToUserId().equals(user.getId())))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());

        return logs.stream().map(log -> {
            Map<String, Object> notif = new HashMap<>();
            notif.put("id", log.getId());
            notif.put("title", "Update on Product");

            // Try to find product name
            String productName = productRepository.findById(log.getProductId())
                    .map(Product::getCropName).orElse("Unknown Product");

            notif.put("body", "Activity: " + log.getNotes() + " for " + productName);
            notif.put("time", log.getTimestamp().toString());
            notif.put("read", false); // Logic for read/unread could be added later
            return notif;
        }).collect(Collectors.toList());
    }
}

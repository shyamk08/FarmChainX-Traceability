package com.farmchainX.farmchainX.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.farmchainX.farmchainX.dto.AdminAnalyticsDTO;
import com.farmchainX.farmchainX.dto.AdminOverview;
import com.farmchainX.farmchainX.dto.SystemLogDTO;
import com.farmchainX.farmchainX.dto.SystemSettingsDTO;
import com.farmchainX.farmchainX.model.AdminPromotionRequest;
import com.farmchainX.farmchainX.model.Role;
import com.farmchainX.farmchainX.model.User;
import com.farmchainX.farmchainX.repository.RoleRepository;
import com.farmchainX.farmchainX.repository.UserRepository;
import com.farmchainX.farmchainX.service.AdminOverviewService;
import com.farmchainX.farmchainX.service.AdminPromotionService;
import com.farmchainX.farmchainX.model.SupplyChainLog;
import com.farmchainX.farmchainX.repository.SupplyChainLogRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminOverviewService overviewService;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final AdminPromotionService promotionService;
    private final SupplyChainLogRepository logRepo;

    record NestedUser(String name, String email) {

    }

    record PromotionRequestView(Long id, LocalDateTime requestedAt, NestedUser user) {

        static PromotionRequestView from(AdminPromotionRequest r) {
            return new PromotionRequestView(
                    r.getId(),
                    r.getRequestedAt(),
                    new NestedUser(r.getUser().getName(), r.getUser().getEmail()));
        }
    }

    public AdminController(AdminOverviewService overviewService,
            UserRepository userRepo,
            RoleRepository roleRepo,
            AdminPromotionService promotionService,
            SupplyChainLogRepository logRepo) {
        this.overviewService = overviewService;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.promotionService = promotionService;
        this.logRepo = logRepo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overview")
    public AdminOverview getOverview() {
        return overviewService.getOverview();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/promote/{userId}")
    public String promoteToAdmin(@PathVariable Long userId,
            @AuthenticationPrincipal UserDetails principal) {
        User target = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (principal != null && principal.getUsername().equalsIgnoreCase(target.getEmail())) {
            throw new RuntimeException("Admins cannot promote themselves");
        }
        Role roleAdmin = roleRepo.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role admin is missing"));
        if (target.getRoles().stream().noneMatch(r -> "ROLE_ADMIN".equals(r.getName()))) {
            target.getRoles().add(roleAdmin);
            userRepo.save(target);
            return target.getEmail() + " promoted to Admin";
        }
        return target.getEmail() + " is already a admin";
    }

    @PostMapping("/request-admin")
    @PreAuthorize("hasAnyRole('ADMIN','CONSUMER','FARMER','RETAILER')")
    public String requestAdminAccess(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Unauthorized - Please log in");
        }
        String email = auth.getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        promotionService.requestAdminAccess(user.getId());
        return "Admin access requested. Waiting for approval.";
    }

    @GetMapping("/promotion-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PromotionRequestView> getPendingRequests() {
        return promotionService.getPendingRequests()
                .stream()
                .map(PromotionRequestView::from)
                .toList();
    }

    @PostMapping("/promotion-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PromotionRequestView> approveRequest(@PathVariable Long requestId,
            Authentication auth) {
        User admin = userRepo.findByEmail(auth.getName()).orElseThrow();
        promotionService.approveRequest(requestId, admin);
        return promotionService.getPendingRequests().stream().map(PromotionRequestView::from).toList();
    }

    @PostMapping("/promotion-requests/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PromotionRequestView> rejectRequest(@PathVariable Long requestId) {
        promotionService.rejectRequest(requestId);
        return promotionService.getPendingRequests().stream().map(PromotionRequestView::from).toList();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SystemLogDTO> getAllLogs() {
        return logRepo.findAll().stream()
                .map(log -> {
                    String action = "System Update";
                    if (log.getPrevHash() == null || "0".equals(log.getPrevHash())) {
                        action = "Product Created";
                    } else if (log.isConfirmed()) {
                        action = "Transfer Confirmed";
                    } else if (log.isRejected()) {
                        action = "Transfer Rejected";
                    } else if (log.getToUserId() != null) {
                        action = "Transfer Initiated";
                    }

                    String details = log.getNotes() != null ? log.getNotes() : "Logged at " + log.getLocation();
                    Long actorId = log.getFromUserId() != null ? log.getFromUserId() : 0L;

                    return new SystemLogDTO(
                            log.getId(),
                            log.getTimestamp().toString(),
                            action,
                            log.getProductId(),
                            actorId,
                            details);
                })
                .toList();
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminAnalyticsDTO getAnalytics() {
        return overviewService.getAnalytics();
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public SystemSettingsDTO getSettings() {
        return overviewService.getSystemSettings();
    }

    @PostMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public SystemSettingsDTO updateSettings(
            @org.springframework.web.bind.annotation.RequestBody SystemSettingsDTO settings) {
        return overviewService.updateSystemSettings(settings);
    }
}

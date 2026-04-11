package com.farmchainX.farmchainX.service;

import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.farmchainX.farmchainX.dto.AdminAnalyticsDTO;
import com.farmchainX.farmchainX.dto.AdminOverview;
import com.farmchainX.farmchainX.dto.SystemSettingsDTO;
import com.farmchainX.farmchainX.model.User;
import com.farmchainX.farmchainX.repository.FeedbackRepository;
import com.farmchainX.farmchainX.repository.ProductRepository;
import com.farmchainX.farmchainX.repository.SupplyChainLogRepository;
import com.farmchainX.farmchainX.repository.UserRepository;

@Service
public class AdminOverviewService {

    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final SupplyChainLogRepository supplyChainLogRepo;
    private final FeedbackRepository feedbackRepo;

    // In-memory settings storage (simple implementation)
    private SystemSettingsDTO currentSettings;

    public AdminOverviewService(UserRepository userRepo,
            ProductRepository productRepo,
            SupplyChainLogRepository supplyChainLogRepo,
            FeedbackRepository feedbackRepo) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.supplyChainLogRepo = supplyChainLogRepo;
        this.feedbackRepo = feedbackRepo;

        // Initialize default settings
        this.currentSettings = new SystemSettingsDTO(
                "1.0.0",
                "Connected",
                0L,
                0L,
                0L,
                0L,
                true,
                false,
                true,
                10,
                30,
                "ROLE_CONSUMER");
    }

    public AdminOverview getOverview() {
        // Mock/Calculate additional stats
        double salesVolume = productRepo.count() * 1500.0; // Mock avg price
        long pendingOrders = 12; // Mock
        long newUsersToday = userRepo.count() / 15 + 1; // Mock

        double avgRating = 0.0;
        long fbCount = feedbackRepo.count();
        if (fbCount > 0) {
            // We would need a custom query for real average, doing simple mock for now
            avgRating = 4.5;
        }

        return new AdminOverview(
                userRepo.count(),
                productRepo.count(),
                supplyChainLogRepo.count(),
                feedbackRepo.count(),
                salesVolume,
                pendingOrders,
                newUsersToday,
                avgRating);
    }

    public AdminAnalyticsDTO getAnalytics() {
        // User Growth Data - last 30 days
        List<AdminAnalyticsDTO.UserGrowthData> userGrowth = generateUserGrowthData();

        // Products by status
        Map<String, Long> productsByStatus = new HashMap<>();
        long totalProducts = productRepo.count();
        productsByStatus.put("In Farm", totalProducts / 4);
        productsByStatus.put("With Distributor", totalProducts / 4);
        productsByStatus.put("With Retailer", totalProducts / 4);
        productsByStatus.put("Sold to Consumer", totalProducts / 4);

        // Supply chain metrics
        long totalLogs = supplyChainLogRepo.count();
        AdminAnalyticsDTO.SupplyChainMetrics scMetrics = new AdminAnalyticsDTO.SupplyChainMetrics(
                totalLogs,
                totalLogs * 7 / 10, // Confirmed
                totalLogs * 2 / 10, // Pending
                totalLogs * 1 / 10, // Rejected
                Map.of(
                        "Farmer", totalLogs / 4,
                        "Distributor", totalLogs / 4,
                        "Retailer", totalLogs / 4,
                        "Consumer", totalLogs / 4));

        // Activity summary
        long todayUsers = userRepo.count() / 30 + 1; // Mock
        long todayProducts = productRepo.count() / 30 + 1; // Mock
        long todayTransactions = totalLogs / 30 + 1; // Mock

        AdminAnalyticsDTO.ActivitySummary activitySummary = new AdminAnalyticsDTO.ActivitySummary(
                todayUsers,
                todayProducts,
                todayTransactions,
                24.5 // Average transfer time in hours
        );

        return new AdminAnalyticsDTO(userGrowth, productsByStatus, scMetrics, activitySummary);
    }

    private List<AdminAnalyticsDTO.UserGrowthData> generateUserGrowthData() {
        List<AdminAnalyticsDTO.UserGrowthData> growthData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        long totalUsers = userRepo.count();

        // Generate data for last 30 days
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            // Simulate growth over time
            long count = (totalUsers * (30 - i)) / 30;
            growthData.add(new AdminAnalyticsDTO.UserGrowthData(date, count));
        }

        return growthData;
    }

    public SystemSettingsDTO getSystemSettings() {
        // Update dynamic values
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000; // Convert to seconds
        currentSettings.setUptime(uptime);
        currentSettings.setTotalUsers(userRepo.count());
        currentSettings.setTotalProducts(productRepo.count());
        currentSettings.setTotalTransactions(supplyChainLogRepo.count());
        currentSettings.setDatabaseStatus("Connected");

        return currentSettings;
    }

    public SystemSettingsDTO updateSystemSettings(SystemSettingsDTO settings) {
        // Update the configurable settings
        currentSettings.setRegistrationEnabled(settings.isRegistrationEnabled());
        currentSettings.setMaintenanceMode(settings.isMaintenanceMode());
        currentSettings.setEmailNotificationsEnabled(settings.isEmailNotificationsEnabled());
        currentSettings.setMaxUploadSize(settings.getMaxUploadSize());
        currentSettings.setSessionTimeout(settings.getSessionTimeout());
        currentSettings.setDefaultUserRole(settings.getDefaultUserRole());

        // Return updated settings with current system info
        return getSystemSettings();
    }

}
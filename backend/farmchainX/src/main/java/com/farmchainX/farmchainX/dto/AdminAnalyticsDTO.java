package com.farmchainX.farmchainX.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AdminAnalyticsDTO {

    // User growth data
    private List<UserGrowthData> userGrowth;

    // Product distribution by status
    private Map<String, Long> productsByStatus;

    // Supply chain activity metrics
    private SupplyChainMetrics supplyChainMetrics;

    // Recent activity summary
    private ActivitySummary activitySummary;

    public AdminAnalyticsDTO() {
    }

    public AdminAnalyticsDTO(List<UserGrowthData> userGrowth,
            Map<String, Long> productsByStatus,
            SupplyChainMetrics supplyChainMetrics,
            ActivitySummary activitySummary) {
        this.userGrowth = userGrowth;
        this.productsByStatus = productsByStatus;
        this.supplyChainMetrics = supplyChainMetrics;
        this.activitySummary = activitySummary;
    }

    // Getters and Setters
    public List<UserGrowthData> getUserGrowth() {
        return userGrowth;
    }

    public void setUserGrowth(List<UserGrowthData> userGrowth) {
        this.userGrowth = userGrowth;
    }

    public Map<String, Long> getProductsByStatus() {
        return productsByStatus;
    }

    public void setProductsByStatus(Map<String, Long> productsByStatus) {
        this.productsByStatus = productsByStatus;
    }

    public SupplyChainMetrics getSupplyChainMetrics() {
        return supplyChainMetrics;
    }

    public void setSupplyChainMetrics(SupplyChainMetrics supplyChainMetrics) {
        this.supplyChainMetrics = supplyChainMetrics;
    }

    public ActivitySummary getActivitySummary() {
        return activitySummary;
    }

    public void setActivitySummary(ActivitySummary activitySummary) {
        this.activitySummary = activitySummary;
    }

    // Inner classes for structured data
    public static class UserGrowthData {
        private LocalDate date;
        private long count;

        public UserGrowthData() {
        }

        public UserGrowthData(LocalDate date, long count) {
            this.date = date;
            this.count = count;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class SupplyChainMetrics {
        private long totalTransfers;
        private long confirmedTransfers;
        private long pendingTransfers;
        private long rejectedTransfers;
        private Map<String, Long> transfersByRole;

        public SupplyChainMetrics() {
        }

        public SupplyChainMetrics(long totalTransfers, long confirmedTransfers,
                long pendingTransfers, long rejectedTransfers,
                Map<String, Long> transfersByRole) {
            this.totalTransfers = totalTransfers;
            this.confirmedTransfers = confirmedTransfers;
            this.pendingTransfers = pendingTransfers;
            this.rejectedTransfers = rejectedTransfers;
            this.transfersByRole = transfersByRole;
        }

        public long getTotalTransfers() {
            return totalTransfers;
        }

        public void setTotalTransfers(long totalTransfers) {
            this.totalTransfers = totalTransfers;
        }

        public long getConfirmedTransfers() {
            return confirmedTransfers;
        }

        public void setConfirmedTransfers(long confirmedTransfers) {
            this.confirmedTransfers = confirmedTransfers;
        }

        public long getPendingTransfers() {
            return pendingTransfers;
        }

        public void setPendingTransfers(long pendingTransfers) {
            this.pendingTransfers = pendingTransfers;
        }

        public long getRejectedTransfers() {
            return rejectedTransfers;
        }

        public void setRejectedTransfers(long rejectedTransfers) {
            this.rejectedTransfers = rejectedTransfers;
        }

        public Map<String, Long> getTransfersByRole() {
            return transfersByRole;
        }

        public void setTransfersByRole(Map<String, Long> transfersByRole) {
            this.transfersByRole = transfersByRole;
        }
    }

    public static class ActivitySummary {
        private long todayRegistrations;
        private long todayProducts;
        private long todayTransactions;
        private double averageTransferTime; // in hours

        public ActivitySummary() {
        }

        public ActivitySummary(long todayRegistrations, long todayProducts,
                long todayTransactions, double averageTransferTime) {
            this.todayRegistrations = todayRegistrations;
            this.todayProducts = todayProducts;
            this.todayTransactions = todayTransactions;
            this.averageTransferTime = averageTransferTime;
        }

        public long getTodayRegistrations() {
            return todayRegistrations;
        }

        public void setTodayRegistrations(long todayRegistrations) {
            this.todayRegistrations = todayRegistrations;
        }

        public long getTodayProducts() {
            return todayProducts;
        }

        public void setTodayProducts(long todayProducts) {
            this.todayProducts = todayProducts;
        }

        public long getTodayTransactions() {
            return todayTransactions;
        }

        public void setTodayTransactions(long todayTransactions) {
            this.todayTransactions = todayTransactions;
        }

        public double getAverageTransferTime() {
            return averageTransferTime;
        }

        public void setAverageTransferTime(double averageTransferTime) {
            this.averageTransferTime = averageTransferTime;
        }
    }
}

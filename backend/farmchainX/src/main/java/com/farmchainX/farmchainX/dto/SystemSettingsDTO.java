package com.farmchainX.farmchainX.dto;

public class SystemSettingsDTO {

    // System information
    private String applicationVersion;
    private String databaseStatus;
    private long uptime; // in seconds
    private long totalUsers;
    private long totalProducts;
    private long totalTransactions;

    // Feature flags
    private boolean registrationEnabled;
    private boolean maintenanceMode;
    private boolean emailNotificationsEnabled;

    // Configuration
    private int maxUploadSize; // in MB
    private int sessionTimeout; // in minutes
    private String defaultUserRole;

    public SystemSettingsDTO() {
    }

    public SystemSettingsDTO(String applicationVersion, String databaseStatus, long uptime,
            long totalUsers, long totalProducts, long totalTransactions,
            boolean registrationEnabled, boolean maintenanceMode,
            boolean emailNotificationsEnabled, int maxUploadSize,
            int sessionTimeout, String defaultUserRole) {
        this.applicationVersion = applicationVersion;
        this.databaseStatus = databaseStatus;
        this.uptime = uptime;
        this.totalUsers = totalUsers;
        this.totalProducts = totalProducts;
        this.totalTransactions = totalTransactions;
        this.registrationEnabled = registrationEnabled;
        this.maintenanceMode = maintenanceMode;
        this.emailNotificationsEnabled = emailNotificationsEnabled;
        this.maxUploadSize = maxUploadSize;
        this.sessionTimeout = sessionTimeout;
        this.defaultUserRole = defaultUserRole;
    }

    // Getters and Setters
    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getDatabaseStatus() {
        return databaseStatus;
    }

    public void setDatabaseStatus(String databaseStatus) {
        this.databaseStatus = databaseStatus;
    }

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public int getMaxUploadSize() {
        return maxUploadSize;
    }

    public void setMaxUploadSize(int maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public String getDefaultUserRole() {
        return defaultUserRole;
    }

    public void setDefaultUserRole(String defaultUserRole) {
        this.defaultUserRole = defaultUserRole;
    }
}

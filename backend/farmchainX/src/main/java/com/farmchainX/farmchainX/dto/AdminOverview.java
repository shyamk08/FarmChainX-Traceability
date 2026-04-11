package com.farmchainX.farmchainX.dto;

public class AdminOverview {

	private long totalUsers;
	private long totalProducts;
	private long totalLogs;
	private long totalFeedbacks;

	private double salesVolume;
	private long pendingOrders;
	private long newUsersToday;
	private double averageRating;

	public AdminOverview() {
	}

	public AdminOverview(long totalUsers, long totalProducts, long totalLogs, long totalFeedbacks,
			double salesVolume, long pendingOrders, long newUsersToday, double averageRating) {
		this.totalUsers = totalUsers;
		this.totalProducts = totalProducts;
		this.totalLogs = totalLogs;
		this.totalFeedbacks = totalFeedbacks;
		this.salesVolume = salesVolume;
		this.pendingOrders = pendingOrders;
		this.newUsersToday = newUsersToday;
		this.averageRating = averageRating;
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

	public long getTotalLogs() {
		return totalLogs;
	}

	public void setTotalLogs(long totalLogs) {
		this.totalLogs = totalLogs;
	}

	public long getTotalFeedbacks() {
		return totalFeedbacks;
	}

	public void setTotalFeedbacks(long totalFeedbacks) {
		this.totalFeedbacks = totalFeedbacks;
	}

	public double getSalesVolume() {
		return salesVolume;
	}

	public void setSalesVolume(double salesVolume) {
		this.salesVolume = salesVolume;
	}

	public long getPendingOrders() {
		return pendingOrders;
	}

	public void setPendingOrders(long pendingOrders) {
		this.pendingOrders = pendingOrders;
	}

	public long getNewUsersToday() {
		return newUsersToday;
	}

	public void setNewUsersToday(long newUsersToday) {
		this.newUsersToday = newUsersToday;
	}

	public double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(double averageRating) {
		this.averageRating = averageRating;
	}

}
package com.farmchainX.farmchainX.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "retailer_inventory")
public class RetailerInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retailer_id", nullable = false)
    private Long retailerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "status", nullable = false)
    private String status; // IN_STOCK, OUT_OF_STOCK, LOW_STOCK

    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;

    @Column(name = "source_distributor_id")
    private Long sourceDistributorId;

    @Column(name = "price_per_unit")
    private Double pricePerUnit;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    public RetailerInventory() {
        this.receivedDate = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.status = "IN_STOCK";
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRetailerId() {
        return retailerId;
    }

    public void setRetailerId(Long retailerId) {
        this.retailerId = retailerId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
        this.lastUpdated = LocalDateTime.now();

        // Auto-update status based on quantity
        if (quantity == null || quantity <= 0) {
            this.status = "OUT_OF_STOCK";
        } else if (quantity < 10) {
            this.status = "LOW_STOCK";
        } else {
            this.status = "IN_STOCK";
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.lastUpdated = LocalDateTime.now();
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
    }

    public Long getSourceDistributorId() {
        return sourceDistributorId;
    }

    public void setSourceDistributorId(Long sourceDistributorId) {
        this.sourceDistributorId = sourceDistributorId;
    }

    public Double getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(Double pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
        this.lastUpdated = LocalDateTime.now();
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}

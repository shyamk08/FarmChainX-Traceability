package com.farmchainX.farmchainX.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "ai_predictions")
public class AIPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    @JsonIgnore
    private User farmer;

    @Column(name = "prediction_data", columnDefinition = "TEXT")
    private String predictionData; // JSON string containing the full prediction

    @Column(name = "quality_grade")
    private String qualityGrade;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(name = "confidence")
    private Integer confidence;

    @Column(name = "market_readiness")
    private String marketReadiness;

    @Column(name = "storage_recommendation", columnDefinition = "TEXT")
    private String storageRecommendation;

    @Column(name = "optimal_selling_window")
    private String optimalSellingWindow;

    @Column(name = "price_estimate")
    private String priceEstimate;

    @Column(name = "certification_eligibility")
    private String certificationEligibility;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AIPrediction() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public User getFarmer() {
        return farmer;
    }

    public void setFarmer(User farmer) {
        this.farmer = farmer;
    }

    public String getPredictionData() {
        return predictionData;
    }

    public void setPredictionData(String predictionData) {
        this.predictionData = predictionData;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public String getMarketReadiness() {
        return marketReadiness;
    }

    public void setMarketReadiness(String marketReadiness) {
        this.marketReadiness = marketReadiness;
    }

    public String getStorageRecommendation() {
        return storageRecommendation;
    }

    public void setStorageRecommendation(String storageRecommendation) {
        this.storageRecommendation = storageRecommendation;
    }

    public String getOptimalSellingWindow() {
        return optimalSellingWindow;
    }

    public void setOptimalSellingWindow(String optimalSellingWindow) {
        this.optimalSellingWindow = optimalSellingWindow;
    }

    public String getPriceEstimate() {
        return priceEstimate;
    }

    public void setPriceEstimate(String priceEstimate) {
        this.priceEstimate = priceEstimate;
    }

    public String getCertificationEligibility() {
        return certificationEligibility;
    }

    public void setCertificationEligibility(String certificationEligibility) {
        this.certificationEligibility = certificationEligibility;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


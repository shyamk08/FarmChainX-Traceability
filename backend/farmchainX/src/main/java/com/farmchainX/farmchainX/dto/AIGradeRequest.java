package com.farmchainX.farmchainX.dto;

public class AIGradeRequest {
    private String imageUrl;
    private Double colorScore;
    private Double freshnessScore;
    private Double sizeScore;
    private Double price; // used for fraud detection

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Double getColorScore() { return colorScore; }
    public void setColorScore(Double colorScore) { this.colorScore = colorScore; }

    public Double getFreshnessScore() { return freshnessScore; }
    public void setFreshnessScore(Double freshnessScore) { this.freshnessScore = freshnessScore; }

    public Double getSizeScore() { return sizeScore; }
    public void setSizeScore(Double sizeScore) { this.sizeScore = sizeScore; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}

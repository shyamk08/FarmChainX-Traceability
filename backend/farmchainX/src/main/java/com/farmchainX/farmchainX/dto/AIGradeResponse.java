package com.farmchainX.farmchainX.dto;

public class AIGradeResponse {
    private String grade;
    private Double confidence;
    private String message;
    private Boolean fraudRisk;

    public AIGradeResponse(String grade, Double confidence, String message, Boolean fraudRisk) {
        this.grade = grade;
        this.confidence = confidence;
        this.message = message;
        this.fraudRisk = fraudRisk;
    }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getFraudRisk() { return fraudRisk; }
    public void setFraudRisk(Boolean fraudRisk) { this.fraudRisk = fraudRisk; }
}

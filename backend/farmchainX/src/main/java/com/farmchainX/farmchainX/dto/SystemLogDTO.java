package com.farmchainX.farmchainX.dto;

public class SystemLogDTO {
    private Long id;
    private String timestamp;
    private String action;
    private Long productId;
    private Long actorId;
    private String details;

    public SystemLogDTO(Long id, String timestamp, String action, Long productId, Long actorId, String details) {
        this.id = id;
        this.timestamp = timestamp;
        this.action = action;
        this.productId = productId;
        this.actorId = actorId;
        this.details = details;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

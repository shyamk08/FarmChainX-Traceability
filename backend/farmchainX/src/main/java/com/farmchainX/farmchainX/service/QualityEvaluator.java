package com.farmchainX.farmchainX.service;

import com.farmchainX.farmchainX.dto.AIGradeRequest;
import com.farmchainX.farmchainX.dto.AIGradeResponse;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class QualityEvaluator {

    private final Random random = new Random();

    public AIGradeResponse evaluate(AIGradeRequest request) {
        double finalScore = 0.0;
        double confidence = 0.80 + (random.nextDouble() * 0.19); // 80% to 99%
        boolean fraudRisk = false;
        String message;
        String grade;

        // If an image URL is provided, we simulate ML inference
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            // Simulate ML based on filename or just randomness for demo
            finalScore = 60.0 + (random.nextDouble() * 40.0); // 60 to 100
        } else {
            // Rule-based logic based on parameters
            double color = request.getColorScore() != null ? request.getColorScore() : 80.0;
            double freshness = request.getFreshnessScore() != null ? request.getFreshnessScore() : 80.0;
            double size = request.getSizeScore() != null ? request.getSizeScore() : 80.0;
            
            // Weighted average
            finalScore = (color * 0.4) + (freshness * 0.4) + (size * 0.2);
        }

        // Fraud detection logic (simple rule: if score is very high but price is suspiciously low)
        if (finalScore >= 90.0 && request.getPrice() != null && request.getPrice() < 20.0) {
            fraudRisk = true;
        }

        // Assign Grade based on finalScore
        if (finalScore >= 80.0) {
            grade = "A";
            message = "High quality product. Excellent freshness and color.";
        } else if (finalScore >= 50.0) {
            grade = "B";
            message = "Medium quality product. Acceptable for market.";
        } else {
            grade = "C";
            message = "Low quality product. Consider processing or discount.";
        }

        return new AIGradeResponse(grade, Math.round(confidence * 100.0) / 100.0, message, fraudRisk);
    }
}

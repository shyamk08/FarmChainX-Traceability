package com.farmchainX.farmchainX.controller;

import com.farmchainX.farmchainX.dto.AIGradeRequest;
import com.farmchainX.farmchainX.dto.AIGradeResponse;
import com.farmchainX.farmchainX.service.QualityEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final QualityEvaluator qualityEvaluator;

    @Autowired
    public AIController(QualityEvaluator qualityEvaluator) {
        this.qualityEvaluator = qualityEvaluator;
    }

    /**
     * Endpoint: POST /api/ai/grade
     * Accepts: imageUrl OR (colorScore, freshnessScore, sizeScore) + optional price for fraud detection.
     * Returns: { grade, confidence, message, fraudRisk }
     *
     * Designed to be swapped with TensorFlow/OpenCV in the future by replacing QualityEvaluator logic.
     */
    @PostMapping("/grade")
    @PreAuthorize("hasAnyRole('FARMER', 'DISTRIBUTOR', 'RETAILER', 'CONSUMER', 'ADMIN')")
    public ResponseEntity<AIGradeResponse> gradeCrop(@RequestBody AIGradeRequest request) {
        AIGradeResponse response = qualityEvaluator.evaluate(request);
        return ResponseEntity.ok(response);
    }
}

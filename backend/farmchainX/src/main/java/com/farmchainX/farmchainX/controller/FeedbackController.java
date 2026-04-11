package com.farmchainX.farmchainX.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.farmchainX.farmchainX.dto.FeedbackRequest;
import com.farmchainX.farmchainX.model.Feedback;
import com.farmchainX.farmchainX.repository.UserRepository;
import com.farmchainX.farmchainX.service.FeedbackService;

@RestController
@RequestMapping("/api/products")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository;

    public FeedbackController(FeedbackService feedbackService, UserRepository userRepository) {
        this.feedbackService = feedbackService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('CONSUMER')")
    @PostMapping("/{id}/feedback")
    public ResponseEntity<?> addFeedback(@PathVariable Long id, @RequestBody FeedbackRequest feedback) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            Long consumerId = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"))
                    .getId();

            Feedback saved = feedbackService.addFeedback(id, consumerId, feedback);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException re) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", re.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/{id}/feedbacks")
    public ResponseEntity<?> getFeedbacks(@PathVariable Long id) {
        try {
            List<Feedback> feedbacks = feedbackService.getFeedbackForProduct(id);
            return ResponseEntity.ok(feedbacks);
        } catch (RuntimeException re) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", re.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Internal server error"));
        }
    }
}
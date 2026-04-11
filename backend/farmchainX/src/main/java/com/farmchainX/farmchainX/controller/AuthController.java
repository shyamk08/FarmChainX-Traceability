package com.farmchainX.farmchainX.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.farmchainX.farmchainX.dto.AuthResponse;
import com.farmchainX.farmchainX.dto.LoginRequest;
import com.farmchainX.farmchainX.dto.RegisterRequest;
import com.farmchainX.farmchainX.dto.TokenRefreshRequest;
import com.farmchainX.farmchainX.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "https://farmchainx-frontend.vercel.app"
}, allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE,
        RequestMethod.OPTIONS }, allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            log.info("Registration request for email: {}", registerRequest.getEmail());
            AuthResponse response = authService.register(registerRequest);
            log.info("User registered successfully: {}", registerRequest.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage(), e);
            return buildErrorResponse(e);
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for user: {}", loginRequest.getEmail());
            AuthResponse response = authService.login(loginRequest);
            log.info("User logged in successfully: {}", loginRequest.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login error for user {}: {}", loginRequest.getEmail(), e.getMessage());
            return buildErrorResponse(e);
        }
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh JWT token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        try {
            log.info("Token refresh requested");
            AuthResponse response = authService.refreshToken(request.getRefreshToken());
            log.info("Token refreshed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            return buildErrorResponse(e);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate token")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authService.logout(token);
                log.info("User logged out successfully");
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return buildErrorResponse(e);
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user information")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.ok(authentication.getPrincipal());
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            return buildErrorResponse(e);
        }
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(Exception e) {
        Map<String, String> errorResponse = new HashMap<>();

        if (e instanceof org.springframework.web.server.ResponseStatusException statusEx) {
            String reason = statusEx.getReason();
            if (reason == null || reason.isEmpty()) {
                // Get reason phrase from HttpStatus enum if available
                int statusCodeValue = statusEx.getStatusCode().value();
                HttpStatus httpStatus = HttpStatus.resolve(statusCodeValue);
                reason = httpStatus != null ? httpStatus.getReasonPhrase() : "Error";
            }
            errorResponse.put("error", reason);
            errorResponse.put("message", reason);
            return ResponseEntity.status(statusEx.getStatusCode()).body(errorResponse);
        }

        errorResponse.put("error", "An unexpected error occurred");
        errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

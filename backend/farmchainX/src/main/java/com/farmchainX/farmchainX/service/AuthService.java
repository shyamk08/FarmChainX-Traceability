package com.farmchainX.farmchainX.service;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.farmchainX.farmchainX.Security.JwtUtil;
import com.farmchainX.farmchainX.dto.AuthResponse;
import com.farmchainX.farmchainX.dto.LoginRequest;
import com.farmchainX.farmchainX.dto.RegisterRequest;
import com.farmchainX.farmchainX.model.Role;
import com.farmchainX.farmchainX.model.User;
import com.farmchainX.farmchainX.repository.RoleRepository;
import com.farmchainX.farmchainX.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        // Validate name
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }

        // Normalize email to lowercase
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;

        if (email == null || email.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        // Validate password
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        if (request.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }

        // Validate role
        if (request.getRole() == null || request.getRole().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        // Use case-insensitive check to prevent duplicate emails with different cases
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists!");
        }

        String roleInput = request.getRole().trim().toUpperCase();

        if (!Set.of("CONSUMER", "FARMER", "DISTRIBUTOR", "RETAILER")
                .contains(roleInput)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only Consumer, Farmer, Distributor, Retailer allowed");
        }

        Role role = roleRepository.findByName("ROLE_" + roleInput)
                .orElseThrow(() -> new RuntimeException("Role not found in DB"));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(role));
        userRepository.save(user);

        return new AuthResponse(null, role.getName(), email, request.getName());
    }

    public AuthResponse login(LoginRequest login) {
        // Validate and trim email
        if (login.getEmail() == null || login.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (login.getPassword() == null || login.getPassword().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        String email = login.getEmail().trim().toLowerCase();
        // Use case-insensitive lookup to handle existing users with mixed-case emails
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        // Verify password - BCrypt handles the comparison securely
        if (!passwordEncoder.matches(login.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // ✅ Check if user has admin role
        boolean isAdmin = user.getRoles()
                .stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        // ✅ Primary role: ADMIN if present, else first assigned role
        String primaryRole = isAdmin
                ? "ROLE_ADMIN"
                : user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .findFirst()
                        .orElse("ROLE_CONSUMER");

        // ✅ Generate token
        String token = jwtUtil.generateToken(user.getEmail(), primaryRole, user.getId());

        return new AuthResponse(token, primaryRole, user.getEmail(), user.getName());
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }

        // Validate refresh token and extract username
        try {
            if (!jwtUtil.validateToken(refreshToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            String email = jwtUtil.extractUsername(refreshToken);
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            // Determine primary role
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
            String primaryRole = isAdmin
                    ? "ROLE_ADMIN"
                    : user.getRoles().stream()
                            .map(Role::getName)
                            .findFirst()
                            .orElse("ROLE_CONSUMER");

            // Generate new access token
            String newToken = jwtUtil.generateToken(user.getEmail(), primaryRole, user.getId());

            return new AuthResponse(newToken, primaryRole, user.getEmail(), user.getName());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
    }

    public void logout(String token) {
        // For stateless JWT, logout is typically handled client-side by removing the
        // token
        // If you want to implement token blacklisting, you would need Redis or similar
        // For now, this is a no-op as JWT tokens are stateless
    }
}

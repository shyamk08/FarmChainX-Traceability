package com.farmchainX.farmchainX.jwt;

import com.farmchainX.farmchainX.Security.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        System.out.println("🧩 [JWT Filter] Running for path: " + path);

        // ✅ Allow public paths to continue to the controller
        if (isPublicPath(path)) {
            System.out.println("⚪ [JWT Filter] Public path, skipping token check");
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // ✅ Allow requests without token to continue
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("⚪ [JWT Filter] No JWT token provided");
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            String email = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            if (role != null && !role.toUpperCase().startsWith("ROLE_")) {
                role = "ROLE_" + role.toUpperCase();
            }

            System.out.println("🟢 [JWT Filter] Token detected. User: " + email + " | Role: " + role);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority(role.trim().toUpperCase()));

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                System.out.println("✅ [JWT Filter] Authenticated user: " + email + " with role: " + role);
            }

        } catch (JwtException ex) {
            System.out.println("❌ [JWT Filter] Invalid JWT: " + ex.getMessage());
            // We still pass forward so that controller returns proper error
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth")
                || path.startsWith("/uploads")
                || path.contains("/qrcode/download");
    }
}
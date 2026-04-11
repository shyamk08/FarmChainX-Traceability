package com.farmchainX.farmchainX.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@ConditionalOnProperty(name = "app.spa.enabled", havingValue = "true", matchIfMissing = true)
public class FrontendController {

    // 1️⃣ Specific routes
    @GetMapping(value = {
            "/", "/dashboard", "/upload", "/products/my", "/scanner",
            "/verify/{uuid:^[0-9a-fA-F\\-]{36}$}",
            "/verify/**",
            "/login", "/register"
    })
    public ResponseEntity<Resource> serveFrontend() {
        Resource indexHtml = new ClassPathResource("static/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml);
    }

    // 2️⃣ SPA fallback (handles all other non-file routes)
    @RequestMapping("/{path:[^\\.]+}")
    public ResponseEntity<Resource> fallback() {
        Resource indexHtml = new ClassPathResource("static/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml);
    }

    // 3️⃣ Error fallback
    @GetMapping("/error")
    public ResponseEntity<Resource> error() {
        Resource indexHtml = new ClassPathResource("static/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml);
    }
}

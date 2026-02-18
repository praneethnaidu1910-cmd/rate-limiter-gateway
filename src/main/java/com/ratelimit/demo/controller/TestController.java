package com.ratelimit.demo.controller;

import com.ratelimit.demo.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TestController {
    
    private final RateLimiterService rateLimiterService;
    
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint(@RequestParam String clientId) {
        
        boolean allowed = rateLimiterService.allowRequest(clientId);
        
        if (!allowed) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Rate limit exceeded");
            error.put("message", "Too many requests. Please try again later.");
            error.put("clientId", clientId);
            error.put("remaining", 0);
            error.put("resetIn", rateLimiterService.getResetTime(clientId) + " seconds");
            error.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(error);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Request successful");
        response.put("clientId", clientId);
        response.put("remaining", rateLimiterService.getRemainingRequests(clientId));
        response.put("resetIn", rateLimiterService.getResetTime(clientId) + " seconds");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus(@RequestParam String clientId) {
        
        Map<String, Object> status = new HashMap<>();
        status.put("clientId", clientId);
        status.put("remaining", rateLimiterService.getRemainingRequests(clientId));
        status.put("resetIn", rateLimiterService.getResetTime(clientId) + " seconds");
        status.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(status);
    }
}
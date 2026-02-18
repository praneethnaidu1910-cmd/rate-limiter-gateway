package com.ratelimit.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimiterService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${rate-limiter.default-limit}")
    private int defaultLimit;
    
    @Value("${rate-limiter.window-seconds}")
    private int windowSeconds;
    
    public boolean allowRequest(String clientId) {
        String key = "rate_limit:" + clientId;
        
        String countStr = redisTemplate.opsForValue().get(key);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        
        if (currentCount >= defaultLimit) {
            return false;
        }
        
        if (currentCount == 0) {
            redisTemplate.opsForValue().set(key, "1", windowSeconds, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(key);
        }
        
        return true;
    }
    
    public int getRemainingRequests(String clientId) {
        String key = "rate_limit:" + clientId;
        String countStr = redisTemplate.opsForValue().get(key);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        return Math.max(0, defaultLimit - currentCount);
    }
    
    public long getResetTime(String clientId) {
        String key = "rate_limit:" + clientId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }
}
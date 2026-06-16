package com.cs2agg.processor;

import com.cs2agg.processor.model.Odds;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class OddsCache {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper mapper;

    public OddsCache(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.mapper = new ObjectMapper();
    }

    public void cacheOdds(String matchId, List<Odds> odds) {
        if (matchId == null || odds == null) {
            return;
        }
        String key = "odds:" + matchId;
        try {
            String json = mapper.writeValueAsString(odds);
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(2));
        } catch (Exception e) {
            System.err.println("Failed to cache odds in Redis for match ID " + matchId + ": " + e.getMessage());
        }
    }

    public Optional<List<Odds>> getOdds(String matchId) {
        if (matchId == null) {
            return Optional.empty();
        }
        String key = "odds:" + matchId;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.trim().isEmpty()) {
                return Optional.empty();
            }
            List<Odds> odds = mapper.readValue(json, new TypeReference<List<Odds>>() {});
            return Optional.of(odds);
        } catch (Exception e) {
            System.err.println("Failed to get odds from Redis for match ID " + matchId + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}

package com.cs2agg.api;

import com.cs2agg.api.model.OddsResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.List;
import java.util.Optional;

public class RedisReader {
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final ObjectMapper mapper;

    public RedisReader() {
        this.mapper = new ObjectMapper();
        String redisHost = System.getenv("REDIS_HOST");
        if (redisHost == null || redisHost.trim().isEmpty()) {
            redisHost = "localhost";
        }
        
        this.redisClient = RedisClient.create("redis://" + redisHost + ":6379");
        this.connection = this.redisClient.connect();
    }

    public Optional<List<OddsResponse>> getOdds(String matchId) {
        if (matchId == null) {
            return Optional.empty();
        }
        try {
            RedisCommands<String, String> syncCommands = connection.sync();
            String json = syncCommands.get("odds:" + matchId);
            if (json == null || json.trim().isEmpty()) {
                return Optional.empty();
            }
            List<OddsResponse> odds = mapper.readValue(json, new TypeReference<List<OddsResponse>>() {});
            return Optional.of(odds);
        } catch (Exception e) {
            System.err.println("Failed to read odds from Redis for match ID " + matchId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    public void close() {
        try {
            connection.close();
            redisClient.shutdown();
        } catch (Exception e) {
            System.err.println("Failed to shutdown Redis client: " + e.getMessage());
        }
    }
}

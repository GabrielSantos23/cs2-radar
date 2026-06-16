package com.cs2agg.processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BracketMatch(
    @JsonProperty("id") String id,
    @JsonProperty("team1") Team team1,
    @JsonProperty("team2") Team team2,
    @JsonProperty("score1") Integer score1,
    @JsonProperty("score2") Integer score2,
    @JsonProperty("status") String status,
    @JsonProperty("scheduledAt") String scheduledAt
) {}

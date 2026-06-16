package com.cs2agg.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BracketResponse(
    @JsonProperty("tournamentId") String tournamentId,
    @JsonProperty("rounds") List<RoundResponse> rounds
) {
    public record RoundResponse(
        @JsonProperty("name") String name,
        @JsonProperty("roundNumber") int roundNumber,
        @JsonProperty("matches") List<MatchResponse> matches
    ) {}
    
    public record MatchResponse(
        @JsonProperty("id") String id,
        @JsonProperty("team1") TeamResponse team1,
        @JsonProperty("team2") TeamResponse team2,
        @JsonProperty("score1") Integer score1,
        @JsonProperty("score2") Integer score2,
        @JsonProperty("status") String status,
        @JsonProperty("scheduledAt") String scheduledAt
    ) {}
}

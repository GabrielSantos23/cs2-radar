package com.cs2agg.processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LiveMatchEvent(
    @JsonProperty("id") String id,
    @JsonProperty("status") String status,
    @JsonProperty("team1") Team team1,
    @JsonProperty("team2") Team team2,
    @JsonProperty("score1") int score1,
    @JsonProperty("score2") int score2,
    @JsonProperty("currentGame") CurrentGame currentGame,
    @JsonProperty("tournamentId") String tournamentId,
    @JsonProperty("tournamentName") String tournamentName
) {}

package com.cs2agg.fetcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Odds(
    @JsonProperty("bookmaker") String bookmaker,
    @JsonProperty("team1_win") double team1Win,
    @JsonProperty("team2_win") double team2Win
) {}

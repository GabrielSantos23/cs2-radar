package com.cs2agg.fetcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

public record BookmakerOdds(
    @JsonProperty("bookmaker") @JsonAlias({"key"}) String bookmaker,
    @JsonProperty("team1Win") @JsonAlias({"team1_win"}) double team1Win,
    @JsonProperty("team2Win") @JsonAlias({"team2_win"}) double team2Win
) {}

package com.cs2agg.fetcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record MatchOdds(
    @JsonProperty("eventId") @JsonAlias({"event_id", "id"}) String eventId,
    @JsonProperty("team1Name") @JsonAlias({"team1_name", "home_team", "team1"}) String team1Name,
    @JsonProperty("team2Name") @JsonAlias({"team2_name", "away_team", "team2"}) String team2Name,
    @JsonProperty("commenceTime") @JsonAlias({"commence_time"}) String commenceTime,
    @JsonProperty("bookmakers") List<BookmakerOdds> bookmakers
) {}

package com.cs2agg.processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MatchEvent(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("begin_at") String beginAt,
    @JsonProperty("tournament_name") String tournamentName,
    @JsonProperty("serie_name") String serieName,
    @JsonProperty("team1") Team team1,
    @JsonProperty("team2") Team team2,
    @JsonProperty("odds") List<Odds> odds
) {}

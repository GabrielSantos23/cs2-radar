package com.cs2agg.fetcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CurrentGame(
    @JsonProperty("gameNumber") int gameNumber,
    @JsonProperty("mapName") String mapName,
    @JsonProperty("roundsTeam1") int roundsTeam1,
    @JsonProperty("roundsTeam2") int roundsTeam2
) {}

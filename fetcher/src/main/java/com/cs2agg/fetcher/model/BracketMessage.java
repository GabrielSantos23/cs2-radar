package com.cs2agg.fetcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BracketMessage(
    @JsonProperty("type") String type,
    @JsonProperty("tournamentId") String tournamentId,
    @JsonProperty("brackets") List<Bracket> brackets
) {}

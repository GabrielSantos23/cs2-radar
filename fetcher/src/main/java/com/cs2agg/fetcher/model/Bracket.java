package com.cs2agg.fetcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Bracket(
    @JsonProperty("round") String round,
    @JsonProperty("roundNumber") int roundNumber,
    @JsonProperty("matches") List<BracketMatch> matches
) {}

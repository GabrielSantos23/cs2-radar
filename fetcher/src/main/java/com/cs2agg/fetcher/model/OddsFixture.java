package com.cs2agg.fetcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OddsFixture(
    @JsonProperty("fixtureId") String fixtureId,
    @JsonProperty("participant1Name") String participant1Name,
    @JsonProperty("participant2Name") String participant2Name,
    @JsonProperty("startTime") String startTime,
    @JsonProperty("hasOdds") boolean hasOdds
) {}

package com.cs2agg.api.model;

import java.util.List;

public record MatchResponse(
    String id,
    String name,
    String beginAt,
    String tournamentName,
    String serieName,
    TeamResponse team1,
    TeamResponse team2,
    List<OddsResponse> odds
) {}

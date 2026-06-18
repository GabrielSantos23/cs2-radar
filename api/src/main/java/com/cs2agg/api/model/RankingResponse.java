package com.cs2agg.api.model;

import java.util.List;

public record RankingResponse(
    String generatedAt,
    List<TeamRankResponseEntry> teams
) {}

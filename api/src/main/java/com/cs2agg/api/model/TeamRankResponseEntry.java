package com.cs2agg.api.model;

public record TeamRankResponseEntry(
    int position,
    int previousPosition,
    int change,
    String teamId,
    String teamName,
    String imageUrl,
    double score
) {}

package com.cs2agg.processor.model;

public record TeamRankEntry(
    int position,
    int previousPosition,
    String teamId,
    String teamName,
    String imageUrl,
    double score
) {}

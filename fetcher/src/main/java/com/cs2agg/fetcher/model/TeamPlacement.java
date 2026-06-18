package com.cs2agg.fetcher.model;

public record TeamPlacement(
    String teamId,
    String teamName,
    String imageUrl,
    int finalPosition
) {}

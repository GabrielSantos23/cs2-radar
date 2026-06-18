package com.cs2agg.processor.model;

public record TeamPlacement(
    String teamId,
    String teamName,
    String imageUrl,
    int finalPosition
) {}

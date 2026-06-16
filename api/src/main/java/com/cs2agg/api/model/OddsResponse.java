package com.cs2agg.api.model;

public record OddsResponse(
    String bookmaker,
    double team1Win,
    double team2Win
) {}

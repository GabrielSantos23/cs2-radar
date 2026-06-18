package com.cs2agg.processor.model;

import java.util.List;

public record TournamentResult(
    String tournamentId,
    String tournamentName,
    String tier,
    String endAt,
    List<TeamPlacement> placements
) {}

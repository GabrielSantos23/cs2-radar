package com.cs2agg.processor;

import com.cs2agg.processor.model.TeamPlacement;
import com.cs2agg.processor.model.TeamRankEntry;
import com.cs2agg.processor.model.TournamentResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class RankingCalculator {

    public double calculatePoints(int position, String tier, String tournamentEndAt) {
        if (position <= 0) {
            return 0.0;
        }

        // Get base points
        double basePoints = 0.0;
        if (position == 1) {
            basePoints = 100.0;
        } else if (position == 2) {
            basePoints = 70.0;
        } else if (position <= 4) {
            basePoints = 45.0;
        } else if (position <= 8) {
            basePoints = 25.0;
        } else if (position <= 16) {
            basePoints = 10.0;
        }

        // Tier multiplier
        double multiplier = 1.0;
        if ("s".equalsIgnoreCase(tier)) {
            multiplier = 2.0;
        } else if ("a".equalsIgnoreCase(tier)) {
            multiplier = 1.0;
        }

        // Time decay
        long daysSinceEnd = 0;
        try {
            if (tournamentEndAt != null && !tournamentEndAt.trim().isEmpty()) {
                Instant endInstant = Instant.parse(tournamentEndAt);
                Instant now = Instant.now();
                daysSinceEnd = ChronoUnit.DAYS.between(endInstant, now);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse tournament date: " + tournamentEndAt + ". Error: " + e.getMessage());
        }

        if (daysSinceEnd < 0) {
            daysSinceEnd = 0;
        }

        if (daysSinceEnd > 180) {
            return 0.0;
        }

        double decayFactor = 1.0 - ((double) daysSinceEnd / 180.0);
        return basePoints * multiplier * decayFactor;
    }

    public List<TeamRankEntry> computeRanking(List<TournamentResult> results) {
        Map<String, TeamScoreAggr> aggrMap = new HashMap<>();

        for (TournamentResult result : results) {
            for (TeamPlacement placement : result.placements()) {
                String teamId = placement.teamId();
                if (teamId == null || teamId.trim().isEmpty()) {
                    continue;
                }

                double points = calculatePoints(placement.finalPosition(), result.tier(), result.endAt());
                if (points > 0) {
                    TeamScoreAggr aggr = aggrMap.computeIfAbsent(teamId, k -> new TeamScoreAggr(
                        placement.teamName(),
                        placement.imageUrl()
                    ));
                    aggr.addScore(points);
                }
            }
        }

        List<Map.Entry<String, TeamScoreAggr>> sortedTeams = new ArrayList<>(aggrMap.entrySet());
        // Sort descending by score
        sortedTeams.sort((a, b) -> Double.compare(b.getValue().score, a.getValue().score));

        List<TeamRankEntry> ranking = new ArrayList<>();
        for (int i = 0; i < sortedTeams.size(); i++) {
            Map.Entry<String, TeamScoreAggr> entry = sortedTeams.get(i);
            int position = i + 1;
            ranking.add(new TeamRankEntry(
                position,
                0, // previousPosition will be filled later by SqsConsumer
                entry.getKey(),
                entry.getValue().name,
                entry.getValue().imageUrl,
                Math.round(entry.getValue().score * 10.0) / 10.0 // Round to 1 decimal place
            ));
        }

        return ranking;
    }

    private static class TeamScoreAggr {
        final String name;
        final String imageUrl;
        double score = 0.0;

        TeamScoreAggr(String name, String imageUrl) {
            this.name = name;
            this.imageUrl = imageUrl;
        }

        void addScore(double points) {
            this.score += points;
        }
    }
}

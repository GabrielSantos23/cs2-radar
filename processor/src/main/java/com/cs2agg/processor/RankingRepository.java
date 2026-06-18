package com.cs2agg.processor;

import com.cs2agg.processor.model.RankingEntity;
import com.cs2agg.processor.model.TeamRankEntry;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RankingRepository {
    private final DynamoDbTable<RankingEntity> table;

    public RankingRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("cs2-ranking", TableSchema.fromBean(RankingEntity.class));
    }

    public void saveSnapshot(String date, List<TeamRankEntry> ranking) {
        if (ranking == null) return;
        for (TeamRankEntry entry : ranking) {
            RankingEntity entity = new RankingEntity(date, entry);
            table.putItem(entity);
        }
    }

    public List<TeamRankEntry> getSnapshot(String date) {
        try {
            return table.query(r -> r.queryConditional(
                    QueryConditional.keyEqualTo(k -> k.partitionValue(date))
            )).items().stream()
                    .map(RankingEntity::toEntry)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Failed to query snapshot for date " + date + ": " + e.getMessage());
            return List.of();
        }
    }

    public void saveRawPlacements(com.cs2agg.processor.model.TournamentResult result) {
        if (result == null || result.placements() == null) return;
        for (com.cs2agg.processor.model.TeamPlacement placement : result.placements()) {
            RankingEntity entity = new RankingEntity();
            entity.setSnapshotDate("RAW_PLACEMENT");
            entity.setTeamId(result.tournamentId() + "#" + placement.teamId());
            entity.setTeamName(placement.teamName());
            entity.setImageUrl(placement.imageUrl());
            entity.setPosition(placement.finalPosition());
            entity.setTournamentName(result.tournamentName());
            entity.setTier(result.tier());
            entity.setEndAt(result.endAt());
            entity.setTtl((System.currentTimeMillis() / 1000L) + (180L * 24L * 60L * 60L));
            table.putItem(entity);
        }
    }

    public List<com.cs2agg.processor.model.TournamentResult> loadAllRawPlacements() {
        try {
            List<RankingEntity> entities = table.query(r -> r.queryConditional(
                    QueryConditional.keyEqualTo(k -> k.partitionValue("RAW_PLACEMENT"))
            )).items().stream().collect(Collectors.toList());

            java.util.Map<String, List<RankingEntity>> grouped = entities.stream()
                    .filter(e -> e.getTeamId() != null && e.getTeamId().contains("#"))
                    .collect(Collectors.groupingBy(e -> e.getTeamId().split("#")[0]));

            List<com.cs2agg.processor.model.TournamentResult> results = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, List<RankingEntity>> entry : grouped.entrySet()) {
                String tournamentId = entry.getKey();
                List<RankingEntity> tEntities = entry.getValue();
                if (tEntities.isEmpty()) continue;

                RankingEntity first = tEntities.get(0);
                List<com.cs2agg.processor.model.TeamPlacement> placements = new java.util.ArrayList<>();
                for (RankingEntity entity : tEntities) {
                    String[] parts = entity.getTeamId().split("#");
                    String teamId = parts.length > 1 ? parts[1] : entity.getTeamId();
                    placements.add(new com.cs2agg.processor.model.TeamPlacement(
                        teamId,
                        entity.getTeamName(),
                        entity.getImageUrl(),
                        entity.getPosition() != null ? entity.getPosition() : 0
                    ));
                }

                results.add(new com.cs2agg.processor.model.TournamentResult(
                    tournamentId,
                    first.getTournamentName(),
                    first.getTier(),
                    first.getEndAt(),
                    placements
                ));
            }
            return results;
        } catch (Exception e) {
            System.err.println("Failed to load raw placements: " + e.getMessage());
            return List.of();
        }
    }
}

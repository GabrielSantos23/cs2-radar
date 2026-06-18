package com.cs2agg.api;

import com.cs2agg.api.model.MatchEntity;
import com.cs2agg.api.model.BracketEntity;
import com.cs2agg.api.model.RankingEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DynamoDbReader {
    private final DynamoDbTable<MatchEntity> table;
    private final DynamoDbTable<BracketEntity> bracketsTable;
    private final DynamoDbTable<RankingEntity> rankingTable;

    public DynamoDbReader() {
        String regionStr = System.getenv("AWS_REGION");
        Region region = (regionStr != null && !regionStr.trim().isEmpty()) ? Region.of(regionStr) : Region.US_EAST_1;
        
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        var builder = DynamoDbClient.builder().region(region);
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            builder.endpointOverride(java.net.URI.create(endpoint));
        }
        DynamoDbClient dynamoDbClient = builder.build();
        
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        
        String tableName = System.getenv("DYNAMODB_TABLE");
        if (tableName == null || tableName.trim().isEmpty()) {
            tableName = "cs2-matches";
        }
        
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(MatchEntity.class));
        this.bracketsTable = enhancedClient.table("cs2-brackets", TableSchema.fromBean(BracketEntity.class));
        this.rankingTable = enhancedClient.table("cs2-ranking", TableSchema.fromBean(RankingEntity.class));
    }

    public List<MatchEntity> getUpcomingMatches() {
        return table.scan().items().stream().collect(Collectors.toList());
    }

    public Optional<MatchEntity> getMatchById(String matchId) {
        return table.scan().items().stream()
                .filter(m -> matchId.equals(m.getMatchId()))
                .findFirst();
    }


    public List<String> getTournaments() {
        return table.scan().items().stream()
                .map(MatchEntity::getTournamentId)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<MatchEntity> getMatchesByTeam(String teamId) {
        return table.scan().items().stream()
                .filter(m -> (m.getTeam1() != null && teamId.equals(m.getTeam1().getId())) || 
                             (m.getTeam2() != null && teamId.equals(m.getTeam2().getId())))
                .collect(Collectors.toList());
    }

    public List<BracketEntity> getBracketsByTournament(String tournamentId) {
        return bracketsTable.query(QueryConditional.keyEqualTo(
                k -> k.partitionValue(tournamentId)
        )).items().stream().collect(Collectors.toList());
    }

    public List<RankingEntity> getRankingSnapshot(String date) {
        try {
            return rankingTable.query(QueryConditional.keyEqualTo(
                    k -> k.partitionValue(date)
            )).items().stream().collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Failed to query ranking snapshot for date " + date + ": " + e.getMessage());
            return List.of();
        }
    }

    public Optional<List<RankingEntity>> getLatestRanking() {
        java.time.LocalDate date = java.time.LocalDate.now();
        for (int i = 0; i < 7; i++) {
            String dateStr = date.minusDays(i).toString();
            List<RankingEntity> ranking = getRankingSnapshot(dateStr);
            if (ranking != null && !ranking.isEmpty()) {
                return Optional.of(ranking);
            }
        }
        return Optional.empty();
    }
}

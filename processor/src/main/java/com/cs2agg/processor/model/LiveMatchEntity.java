package com.cs2agg.processor.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class LiveMatchEntity {
    private String tournamentId;
    private String matchId; // "live#{matchId}"
    private String status;
    private Team team1;
    private Team team2;
    private int score1;
    private int score2;
    private CurrentGame currentGame;
    private Long ttl;
    private String updatedAt;

    public LiveMatchEntity() {}

    public LiveMatchEntity(LiveMatchEvent event) {
        this.tournamentId = (event.tournamentName() != null && !event.tournamentName().trim().isEmpty())
                ? event.tournamentName() : "UNKNOWN_TOURNAMENT";
        this.matchId = "live#" + event.id();
        this.status = event.status();
        this.team1 = event.team1();
        this.team2 = event.team2();
        this.score1 = event.score1();
        this.score2 = event.score2();
        this.currentGame = event.currentGame();
        this.ttl = (System.currentTimeMillis() / 1000L) + 1800; // 30 minutes TTL
        this.updatedAt = java.time.Instant.now().toString();
    }

    @DynamoDbPartitionKey
    public String getTournamentId() { return tournamentId; }
    public void setTournamentId(String tournamentId) { this.tournamentId = tournamentId; }

    @DynamoDbSortKey
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Team getTeam1() { return team1; }
    public void setTeam1(Team team1) { this.team1 = team1; }

    public Team getTeam2() { return team2; }
    public void setTeam2(Team team2) { this.team2 = team2; }

    public int getScore1() { return score1; }
    public void setScore1(int score1) { this.score1 = score1; }

    public int getScore2() { return score2; }
    public void setScore2(int score2) { this.score2 = score2; }

    public CurrentGame getCurrentGame() { return currentGame; }
    public void setCurrentGame(CurrentGame currentGame) { this.currentGame = currentGame; }

    public Long getTtl() { return ttl; }
    public void setTtl(Long ttl) { this.ttl = ttl; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}

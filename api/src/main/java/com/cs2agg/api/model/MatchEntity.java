package com.cs2agg.api.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

@DynamoDbBean
public class MatchEntity {
    private String tournamentId;
    private String matchId;
    private String name;
    private String beginAt;
    private String serieName;
    private TeamEntity team1;
    private TeamEntity team2;
    private List<OddsEntity> odds;
    private Long ttl;
    private String pandascoreTournamentId;

    public MatchEntity() {}

    @DynamoDbPartitionKey
    public String getTournamentId() { return tournamentId; }
    public void setTournamentId(String tournamentId) { this.tournamentId = tournamentId; }

    @DynamoDbSortKey
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBeginAt() { return beginAt; }
    public void setBeginAt(String beginAt) { this.beginAt = beginAt; }

    public String getSerieName() { return serieName; }
    public void setSerieName(String serieName) { this.serieName = serieName; }

    public TeamEntity getTeam1() { return team1; }
    public void setTeam1(TeamEntity team1) { this.team1 = team1; }

    public TeamEntity getTeam2() { return team2; }
    public void setTeam2(TeamEntity team2) { this.team2 = team2; }

    public List<OddsEntity> getOdds() { return odds; }
    public void setOdds(List<OddsEntity> odds) { this.odds = odds; }

    public Long getTtl() { return ttl; }
    public void setTtl(Long ttl) { this.ttl = ttl; }

    public String getPandascoreTournamentId() { return pandascoreTournamentId; }
    public void setPandascoreTournamentId(String pandascoreTournamentId) { this.pandascoreTournamentId = pandascoreTournamentId; }
}

package com.cs2agg.processor.model;

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
    private Team team1;
    private Team team2;
    private List<Odds> odds;
    private Long ttl;
    private String pandascoreTournamentId;

    public MatchEntity() {}

    public MatchEntity(MatchEvent event) {
        this.tournamentId = (event.tournamentName() != null && !event.tournamentName().trim().isEmpty()) 
                ? event.tournamentName() : "UNKNOWN_TOURNAMENT";
        this.matchId = event.id();
        this.name = event.name();
        this.beginAt = event.beginAt();
        this.serieName = event.serieName();
        this.team1 = event.team1();
        this.team2 = event.team2();
        this.odds = event.odds();
        this.pandascoreTournamentId = event.tournamentId();
        // Default TTL: 7 days from now (in seconds)
        this.ttl = (System.currentTimeMillis() / 1000L) + (7 * 24 * 60 * 60);
    }

    @DynamoDbPartitionKey
    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }

    @DynamoDbSortKey
    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeginAt() {
        return beginAt;
    }

    public void setBeginAt(String beginAt) {
        this.beginAt = beginAt;
    }

    public String getSerieName() {
        return serieName;
    }

    public void setSerieName(String serieName) {
        this.serieName = serieName;
    }

    public Team getTeam1() {
        return team1;
    }

    public void setTeam1(Team team1) {
        this.team1 = team1;
    }

    public Team getTeam2() {
        return team2;
    }

    public void setTeam2(Team team2) {
        this.team2 = team2;
    }

    public List<Odds> getOdds() {
        return odds;
    }

    public void setOdds(List<Odds> odds) {
        this.odds = odds;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public String getPandascoreTournamentId() {
        return pandascoreTournamentId;
    }

    public void setPandascoreTournamentId(String pandascoreTournamentId) {
        this.pandascoreTournamentId = pandascoreTournamentId;
    }

    public MatchEvent toEvent() {
        return new MatchEvent(matchId, name, beginAt, tournamentId, serieName, team1, team2, odds, pandascoreTournamentId);
    }
}

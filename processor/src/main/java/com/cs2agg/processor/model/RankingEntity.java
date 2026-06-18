package com.cs2agg.processor.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class RankingEntity {
    private String snapshotDate;
    private String teamId;
    private String teamName;
    private String imageUrl;
    private Double score;
    private Integer position;
    private Integer previousPosition;
    private Long ttl;
    
    // Additional attributes for raw tournament results
    private String tournamentName;
    private String tier;
    private String endAt;

    public RankingEntity() {}

    public RankingEntity(String snapshotDate, TeamRankEntry entry) {
        this.snapshotDate = snapshotDate;
        this.teamId = entry.teamId();
        this.teamName = entry.teamName();
        this.imageUrl = entry.imageUrl();
        this.score = entry.score();
        this.position = entry.position();
        this.previousPosition = entry.previousPosition();
        // Expire snapshots older than 90 days (90 * 24 * 60 * 60)
        this.ttl = (System.currentTimeMillis() / 1000L) + (90L * 24L * 60L * 60L);
    }

    @DynamoDbPartitionKey
    public String getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(String snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    @DynamoDbSortKey
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getPreviousPosition() {
        return previousPosition;
    }

    public void setPreviousPosition(Integer previousPosition) {
        this.previousPosition = previousPosition;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getEndAt() {
        return endAt;
    }

    public void setEndAt(String endAt) {
        this.endAt = endAt;
    }

    public TeamRankEntry toEntry() {
        return new TeamRankEntry(
            position != null ? position : 0,
            previousPosition != null ? previousPosition : 0,
            teamId,
            teamName,
            imageUrl,
            score != null ? score : 0.0
        );
    }
}

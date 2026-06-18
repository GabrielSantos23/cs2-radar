package com.cs2agg.api.model;

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

    public RankingEntity() {}

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
}

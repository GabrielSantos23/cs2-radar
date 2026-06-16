package com.cs2agg.api.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class OddsEntity {
    private String bookmaker;
    private double team1Win;
    private double team2Win;

    public OddsEntity() {}

    public OddsEntity(String bookmaker, double team1Win, double team2Win) {
        this.bookmaker = bookmaker;
        this.team1Win = team1Win;
        this.team2Win = team2Win;
    }

    public String getBookmaker() { return bookmaker; }
    public void setBookmaker(String bookmaker) { this.bookmaker = bookmaker; }

    public double getTeam1Win() { return team1Win; }
    public void setTeam1Win(double team1Win) { this.team1Win = team1Win; }

    public double getTeam2Win() { return team2Win; }
    public void setTeam2Win(double team2Win) { this.team2Win = team2Win; }
}

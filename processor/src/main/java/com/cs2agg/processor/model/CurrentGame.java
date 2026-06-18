package com.cs2agg.processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class CurrentGame {
    private int gameNumber;
    private String mapName;
    private int roundsTeam1;
    private int roundsTeam2;

    public CurrentGame() {}

    public CurrentGame(int gameNumber, String mapName, int roundsTeam1, int roundsTeam2) {
        this.gameNumber = gameNumber;
        this.mapName = mapName;
        this.roundsTeam1 = roundsTeam1;
        this.roundsTeam2 = roundsTeam2;
    }

    @JsonProperty("gameNumber")
    public int getGameNumber() { return gameNumber; }
    public void setGameNumber(int gameNumber) { this.gameNumber = gameNumber; }

    @JsonProperty("mapName")
    public String getMapName() { return mapName; }
    public void setMapName(String mapName) { this.mapName = mapName; }

    @JsonProperty("roundsTeam1")
    public int getRoundsTeam1() { return roundsTeam1; }
    public void setRoundsTeam1(int roundsTeam1) { this.roundsTeam1 = roundsTeam1; }

    @JsonProperty("roundsTeam2")
    public int getRoundsTeam2() { return roundsTeam2; }
    public void setRoundsTeam2(int roundsTeam2) { this.roundsTeam2 = roundsTeam2; }
}

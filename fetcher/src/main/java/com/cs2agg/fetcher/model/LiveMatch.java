package com.cs2agg.fetcher.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;

@JsonDeserialize(using = LiveMatch.LiveMatchDeserializer.class)
public record LiveMatch(
    @JsonProperty("id") String id,
    @JsonProperty("status") String status,
    @JsonProperty("team1") Team team1,
    @JsonProperty("team2") Team team2,
    @JsonProperty("score1") int score1,
    @JsonProperty("score2") int score2,
    @JsonProperty("currentGame") CurrentGame currentGame,
    @JsonProperty("tournamentId") String tournamentId,
    @JsonProperty("tournamentName") String tournamentName
) {

    public static class LiveMatchDeserializer extends JsonDeserializer<LiveMatch> {
        @Override
        public LiveMatch deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            
            String id = node.has("id") && !node.get("id").isNull() ? node.get("id").asText() : "";
            String status = node.has("status") && !node.get("status").isNull() ? node.get("status").asText() : "";
            
            Team team1 = null;
            Team team2 = null;
            if (node.has("opponents") && !node.get("opponents").isNull() && node.get("opponents").isArray()) {
                JsonNode opponents = node.get("opponents");
                if (opponents.size() > 0) {
                    team1 = parseTeam(opponents.get(0).get("opponent"));
                }
                if (opponents.size() > 1) {
                    team2 = parseTeam(opponents.get(1).get("opponent"));
                }
            }
            
            int score1 = 0;
            int score2 = 0;
            if (node.has("results") && !node.get("results").isNull() && node.get("results").isArray()) {
                for (JsonNode res : node.get("results")) {
                    String teamId = res.has("team_id") && !res.get("team_id").isNull() ? res.get("team_id").asText() : "";
                    int score = res.has("score") && !res.get("score").isNull() ? res.get("score").asInt() : 0;
                    if (team1 != null && team1.id() != null && team1.id().equals(teamId)) {
                        score1 = score;
                    }
                    if (team2 != null && team2.id() != null && team2.id().equals(teamId)) {
                        score2 = score;
                    }
                }
            }
            
            String tournamentId = "";
            if (node.has("tournament_id") && !node.get("tournament_id").isNull()) {
                tournamentId = node.get("tournament_id").asText();
            }
            
            String tournamentName = "";
            if (node.has("tournament") && !node.get("tournament").isNull()) {
                JsonNode tournament = node.get("tournament");
                if (tournament.has("name") && !tournament.get("name").isNull()) {
                    tournamentName = tournament.get("name").asText();
                }
                if (tournamentId.isEmpty() && tournament.has("id") && !tournament.get("id").isNull()) {
                    tournamentId = tournament.get("id").asText();
                }
            }
            if (tournamentName.isEmpty()) {
                tournamentName = "UNKNOWN_TOURNAMENT";
            }
            
            CurrentGame currentGame = null;
            JsonNode currentGameNode = node.has("current_game") && !node.get("current_game").isNull() ? node.get("current_game") : 
                                      (node.has("currentGame") && !node.get("currentGame").isNull() ? node.get("currentGame") : null);
            
            if (currentGameNode != null) {
                int gameNumber = 0;
                if (currentGameNode.has("game_number")) {
                    gameNumber = currentGameNode.get("game_number").asInt();
                } else if (currentGameNode.has("gameNumber")) {
                    gameNumber = currentGameNode.get("gameNumber").asInt();
                }
                
                String mapName = "";
                if (currentGameNode.has("map_name")) {
                    mapName = currentGameNode.get("map_name").asText();
                } else if (currentGameNode.has("mapName")) {
                    mapName = currentGameNode.get("mapName").asText();
                } else if (currentGameNode.has("map") && !currentGameNode.get("map").isNull()) {
                    JsonNode mapNode = currentGameNode.get("map");
                    if (mapNode.has("name")) {
                        mapName = mapNode.get("name").asText();
                    }
                }
                
                int roundsTeam1 = 0;
                int roundsTeam2 = 0;
                if (currentGameNode.has("rounds_team1")) {
                    roundsTeam1 = currentGameNode.get("rounds_team1").asInt();
                } else if (currentGameNode.has("roundsTeam1")) {
                    roundsTeam1 = currentGameNode.get("roundsTeam1").asInt();
                }
                if (currentGameNode.has("rounds_team2")) {
                    roundsTeam2 = currentGameNode.get("rounds_team2").asInt();
                } else if (currentGameNode.has("roundsTeam2")) {
                    roundsTeam2 = currentGameNode.get("roundsTeam2").asInt();
                }
                
                currentGame = new CurrentGame(gameNumber, mapName, roundsTeam1, roundsTeam2);
            } else if (node.has("games") && !node.get("games").isNull() && node.get("games").isArray()) {
                JsonNode games = node.get("games");
                JsonNode activeGame = null;
                for (JsonNode g : games) {
                    boolean finished = g.has("finished") && g.get("finished").asBoolean();
                    if (!finished) {
                        activeGame = g;
                        break;
                    }
                }
                if (activeGame == null && games.size() > 0) {
                    activeGame = games.get(games.size() - 1);
                }
                
                if (activeGame != null) {
                    int gameNumber = activeGame.has("position") ? activeGame.get("position").asInt() : 1;
                    String mapName = "";
                    if (activeGame.has("map") && !activeGame.get("map").isNull()) {
                        JsonNode mapNode = activeGame.get("map");
                        if (mapNode.has("name")) {
                            mapName = mapNode.get("name").asText();
                        }
                    }
                    
                    int roundsTeam1 = 0;
                    int roundsTeam2 = 0;
                    
                    JsonNode teamsNode = activeGame.has("teams") && !activeGame.get("teams").isNull() ? activeGame.get("teams") : 
                                        (activeGame.has("results") && !activeGame.get("results").isNull() ? activeGame.get("results") : null);
                    if (teamsNode != null && teamsNode.isArray()) {
                        for (JsonNode t : teamsNode) {
                            String teamId = t.has("team_id") ? t.get("team_id").asText() : "";
                            int score = t.has("score") ? t.get("score").asInt() : 0;
                            if (team1 != null && team1.id() != null && team1.id().equals(teamId)) {
                                roundsTeam1 = score;
                            }
                            if (team2 != null && team2.id() != null && team2.id().equals(teamId)) {
                                roundsTeam2 = score;
                            }
                        }
                    }
                    
                    currentGame = new CurrentGame(gameNumber, mapName, roundsTeam1, roundsTeam2);
                }
            }
            
            return new LiveMatch(id, status, team1, team2, score1, score2, currentGame, tournamentId, tournamentName);
        }
        
        private Team parseTeam(JsonNode opponentNode) {
            if (opponentNode == null || opponentNode.isNull()) {
                return null;
            }
            String id = opponentNode.has("id") && !opponentNode.get("id").isNull() ? opponentNode.get("id").asText() : "";
            String name = opponentNode.has("name") && !opponentNode.get("name").isNull() ? opponentNode.get("name").asText() : "";
            String acronym = opponentNode.has("acronym") && !opponentNode.get("acronym").isNull() ? opponentNode.get("acronym").asText() : "";
            String imageUrl = opponentNode.has("image_url") && !opponentNode.get("image_url").isNull() ? opponentNode.get("image_url").asText() : "";
            return new Team(id, name, acronym, imageUrl);
        }
    }
}

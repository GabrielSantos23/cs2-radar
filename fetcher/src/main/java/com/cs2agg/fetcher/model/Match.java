package com.cs2agg.fetcher.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(using = Match.MatchDeserializer.class)
public record Match(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("begin_at") String beginAt,
    @JsonProperty("tournament_name") String tournamentName,
    @JsonProperty("serie_name") String serieName,
    @JsonProperty("team1") Team team1,
    @JsonProperty("team2") Team team2,
    @JsonProperty("odds") List<Odds> odds
) {

    public static class MatchDeserializer extends JsonDeserializer<Match> {
        @Override
        public Match deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            
            String id = node.has("id") && !node.get("id").isNull() ? node.get("id").asText() : "";
            String name = node.has("name") && !node.get("name").isNull() ? node.get("name").asText() : "";
            String beginAt = node.has("begin_at") && !node.get("begin_at").isNull() ? node.get("begin_at").asText() : "";
            
            String tournamentName = "";
            if (node.has("tournament") && !node.get("tournament").isNull()) {
                JsonNode tournament = node.get("tournament");
                if (tournament.has("name") && !tournament.get("name").isNull()) {
                    tournamentName = tournament.get("name").asText();
                }
            }
            
            String serieName = "";
            if (node.has("serie") && !node.get("serie").isNull()) {
                JsonNode serie = node.get("serie");
                if (serie.has("name") && !serie.get("name").isNull()) {
                    serieName = serie.get("name").asText();
                }
            }
            
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
            
            List<Odds> oddsList = new ArrayList<>();
            // Check if PandaScore or other custom bookmaker odds are nested, or if we map them directly
            if (node.has("odds") && !node.get("odds").isNull() && node.get("odds").isArray()) {
                for (JsonNode oddNode : node.get("odds")) {
                    String bookmaker = oddNode.has("bookmaker") && !oddNode.get("bookmaker").isNull() ? oddNode.get("bookmaker").asText() : "";
                    double team1Win = oddNode.has("team1_win") && !oddNode.get("team1_win").isNull() ? oddNode.get("team1_win").asDouble() : 0.0;
                    double team2Win = oddNode.has("team2_win") && !oddNode.get("team2_win").isNull() ? oddNode.get("team2_win").asDouble() : 0.0;
                    oddsList.add(new Odds(bookmaker, team1Win, team2Win));
                }
            }
            
            return new Match(id, name, beginAt, tournamentName, serieName, team1, team2, oddsList);
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

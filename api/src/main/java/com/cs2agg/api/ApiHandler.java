package com.cs2agg.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.cs2agg.api.model.RankingResponse;
import com.cs2agg.api.model.TeamRankResponseEntry;
import com.cs2agg.api.model.RankingEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private final MatchService matchService;
    private final BracketService bracketService;
    private final DynamoDbReader dbReader;
    private final ObjectMapper mapper;

    public ApiHandler() {
        this.dbReader = new DynamoDbReader();
        this.matchService = new MatchService();
        this.bracketService = new BracketService(dbReader);
        this.mapper = new ObjectMapper();
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String routeKey = event.getRouteKey();
        System.out.println("API Lambda handler invoked. RouteKey: " + routeKey);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type");

        try {
            if (routeKey == null) {
                return buildResponse(404, "{\"message\":\"Not Found\"}", headers);
            }

            if (routeKey.equals("GET /matches/upcoming")) {
                var matches = matchService.getUpcoming();
                return buildResponse(200, mapper.writeValueAsString(matches), headers);
                
            } else if (routeKey.equals("GET /matches/{id}")) {
                Map<String, String> pathParams = event.getPathParameters();
                String id = (pathParams != null) ? pathParams.get("id") : null;
                if (id == null || id.trim().isEmpty()) {
                    return buildResponse(400, "{\"message\":\"Missing path parameter 'id'\"}", headers);
                }
                var matchOpt = matchService.getById(id);
                if (matchOpt.isPresent()) {
                    return buildResponse(200, mapper.writeValueAsString(matchOpt.get()), headers);
                } else {
                    return buildResponse(404, "{\"message\":\"Match not found\"}", headers);
                }
                
            } else if (routeKey.equals("GET /tournaments")) {
                var tournaments = matchService.getTournaments();
                return buildResponse(200, mapper.writeValueAsString(tournaments), headers);
                
            } else if (routeKey.equals("GET /teams/{id}/matches")) {
                Map<String, String> pathParams = event.getPathParameters();
                String id = (pathParams != null) ? pathParams.get("id") : null;
                if (id == null || id.trim().isEmpty()) {
                    return buildResponse(400, "{\"message\":\"Missing path parameter 'id'\"}", headers);
                }
                var matches = matchService.getMatchesByTeam(id);
                return buildResponse(200, mapper.writeValueAsString(matches), headers);
                
            } else if (routeKey.equals("GET /tournaments/{id}/bracket")) {
                Map<String, String> pathParams = event.getPathParameters();
                String id = (pathParams != null) ? pathParams.get("id") : null;
                if (id == null || id.trim().isEmpty()) {
                    return buildResponse(400, "{\"message\":\"Missing path parameter 'id'\"}", headers);
                }
                Optional<com.cs2agg.api.model.BracketResponse> bracketOpt = bracketService.getBracketByTournament(id);
                if (bracketOpt.isPresent()) {
                    return buildResponse(200, mapper.writeValueAsString(bracketOpt.get()), headers);
                } else {
                    return buildResponse(404, "{\"message\":\"Bracket not found\"}", headers);
                }

            } else if (routeKey.equals("GET /ranking")) {
                Optional<List<RankingEntity>> latestOpt = dbReader.getLatestRanking();
                if (latestOpt.isPresent() && !latestOpt.get().isEmpty()) {
                    List<RankingEntity> entities = latestOpt.get();
                    String generatedAt = entities.get(0).getSnapshotDate();
                    
                    List<TeamRankResponseEntry> responseEntries = new ArrayList<>();
                    for (RankingEntity entity : entities) {
                        int pos = entity.getPosition() != null ? entity.getPosition() : 0;
                        int prevPos = entity.getPreviousPosition() != null ? entity.getPreviousPosition() : 0;
                        int change = prevPos == 0 ? 0 : prevPos - pos;
                        
                        responseEntries.add(new TeamRankResponseEntry(
                            pos,
                            prevPos,
                            change,
                            entity.getTeamId(),
                            entity.getTeamName(),
                            entity.getImageUrl(),
                            entity.getScore() != null ? entity.getScore() : 0.0
                        ));
                    }
                    responseEntries.sort(java.util.Comparator.comparingInt(TeamRankResponseEntry::position));
                    
                    RankingResponse response = new RankingResponse(generatedAt, responseEntries);
                    return buildResponse(200, mapper.writeValueAsString(response), headers);
                } else {
                    return buildResponse(404, "{\"message\":\"No ranking snapshot found\"}", headers);
                }

            } else {
                return buildResponse(404, "{\"message\":\"Route not found\"}", headers);
            }

        } catch (Exception e) {
            System.err.println("API Handler error: " + e.getMessage());
            e.printStackTrace();
            return buildResponse(500, "{\"message\":\"Internal Server Error\"}", headers);
        }
    }

    private APIGatewayV2HTTPResponse buildResponse(int statusCode, String body, Map<String, String> headers) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withBody(body)
                .withHeaders(headers)
                .build();
    }
}

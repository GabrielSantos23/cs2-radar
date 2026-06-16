package com.cs2agg.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private final MatchService matchService;
    private final BracketService bracketService;
    private final ObjectMapper mapper;

    public ApiHandler() {
        this.matchService = new MatchService();
        this.bracketService = new BracketService(new DynamoDbReader());
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

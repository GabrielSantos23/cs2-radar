package com.cs2agg.api;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalApiServer {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ApiHandler handler = new ApiHandler();
        
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                
                System.out.println("Local API received request: " + method + " " + path);
                
                APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
                event.setRouteKey(null);
                
                Map<String, String> pathParams = new HashMap<>();
                
                if (method.equals("GET") && path.equals("/matches/upcoming")) {
                    event.setRouteKey("GET /matches/upcoming");
                } else if (method.equals("GET") && path.startsWith("/matches/")) {
                    String id = path.substring("/matches/".length());
                    if (!id.contains("/")) {
                        event.setRouteKey("GET /matches/{id}");
                        pathParams.put("id", id);
                    }
                } else if (method.equals("GET") && path.equals("/tournaments")) {
                    event.setRouteKey("GET /tournaments");
                } else if (method.equals("GET") && path.startsWith("/teams/") && path.endsWith("/matches")) {
                    Pattern pattern = Pattern.compile("^/teams/([^/]+)/matches$");
                    Matcher matcher = pattern.matcher(path);
                    if (matcher.matches()) {
                        event.setRouteKey("GET /teams/{id}/matches");
                        pathParams.put("id", matcher.group(1));
                    }
                }
                
                event.setPathParameters(pathParams);
                
                APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
                
                byte[] responseBytes = response.getBody() != null ? response.getBody().getBytes("UTF-8") : new byte[0];
                
                if (response.getHeaders() != null) {
                    response.getHeaders().forEach((k, v) -> exchange.getResponseHeaders().set(k, v));
                }
                
                exchange.sendResponseHeaders(response.getStatusCode(), responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        });
        
        System.out.println("Local API Server started on http://localhost:" + port);
        server.start();
    }
}

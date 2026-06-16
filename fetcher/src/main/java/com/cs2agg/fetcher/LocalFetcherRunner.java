package com.cs2agg.fetcher;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

public class LocalFetcherRunner {
    public static void main(String[] args) {
        System.out.println("Starting Local Fetcher Runner...");
        try {
            try {
                File cacheDir = new File(System.getProperty("user.home"), ".aws/login/cache");
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".json"));
                    if (files != null && files.length > 0) {
                        File cacheFile = files[0];
                        System.out.println("Carregando credenciais do cache: " + cacheFile.getAbsolutePath());
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(cacheFile);
                        if (root.has("accessToken")) {
                            JsonNode tokenNode = root.get("accessToken");
                            String accessKeyId = tokenNode.get("accessKeyId").asText();
                            String secretAccessKey = tokenNode.get("secretAccessKey").asText();
                            String sessionToken = tokenNode.get("sessionToken").asText();
                            
                            System.setProperty("aws.accessKeyId", accessKeyId);
                            System.setProperty("aws.secretAccessKey", secretAccessKey);
                            System.setProperty("aws.sessionToken", sessionToken);
                            System.setProperty("aws.region", "us-east-1");
                            System.out.println("Credenciais do cache configuradas com sucesso!");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Aviso: Falha ao carregar credenciais em cache: " + e.getMessage());
            }

            FetcherHandler handler = new FetcherHandler();
            ScheduledEvent event = new ScheduledEvent();
            event.setId("local-trigger");
            event.setTime(org.joda.time.DateTime.now());
            
            handler.handleRequest(event, null);
            System.out.println("Local Fetcher Runner completed successfully!");
        } catch (Exception e) {
            System.err.println("Local Fetcher Runner failed:");
            e.printStackTrace();
        }
    }
}

package com.cs2agg.fetcher;

import com.cs2agg.fetcher.model.MatchOdds;
import com.cs2agg.fetcher.model.BookmakerOdds;
import com.cs2agg.fetcher.model.OddsFixture;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OddsPapiClient {
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OddsPapiClient() {
        this.apiKey = System.getenv("ODDSPAPI_KEY");
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<OddsFixture> fetchCS2Fixtures() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("ODDSPAPI_KEY environment variable is not set. Skipping fixtures fetch.");
            return Collections.emptyList();
        }

        Instant now = Instant.now();
        String fromStr = now.toString();
        String toStr = now.plus(7, ChronoUnit.DAYS).toString();

        HttpUrl url = HttpUrl.parse("https://api.oddspapi.io/v4/fixtures")
                .newBuilder()
                .addQueryParameter("sportId", "17")
                .addQueryParameter("from", fromStr)
                .addQueryParameter("to", toStr)
                .addQueryParameter("apiKey", apiKey)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to fetch CS2 fixtures from OddsPapi: HTTP " + response.code() + " - " + response.message());
                return Collections.emptyList();
            }

            String body = response.body().string();
            return objectMapper.readValue(body, objectMapper.getTypeFactory().constructCollectionType(List.class, OddsFixture.class));
        } catch (IOException e) {
            System.err.println("Error calling OddsPapi fixtures API: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public MatchOdds fetchOddsForFixture(OddsFixture fixture) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null;
        }

        HttpUrl url = HttpUrl.parse("https://api.oddspapi.io/v4/odds")
                .newBuilder()
                .addQueryParameter("fixtureId", fixture.fixtureId())
                .addQueryParameter("apiKey", apiKey)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to fetch odds for fixture " + fixture.fixtureId() + ": HTTP " + response.code() + " - " + response.message());
                return null;
            }

            String body = response.body().string();
            JsonNode root = objectMapper.readTree(body);
            JsonNode bookmakerOddsNode = root.path("bookmakerOdds");
            
            List<BookmakerOdds> bookmakersList = new ArrayList<>();
            if (bookmakerOddsNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = bookmakerOddsNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String bookmaker = entry.getKey();
                    JsonNode bookmakerNode = entry.getValue();
                    
                    JsonNode marketNode = bookmakerNode.path("markets").path("171"); // Moneyline CS2
                    if (marketNode.isMissingNode()) {
                        continue;
                    }
                    
                    JsonNode outcomesNode = marketNode.path("outcomes");
                    JsonNode outcome1 = outcomesNode.path("171").path("players").path("0");
                    JsonNode outcome2 = outcomesNode.path("172").path("players").path("0");
                    
                    if (!outcome1.isMissingNode() && !outcome2.isMissingNode()) {
                        double team1Win = outcome1.path("price").asDouble();
                        double team2Win = outcome2.path("price").asDouble();
                        bookmakersList.add(new BookmakerOdds(bookmaker, team1Win, team2Win));
                    }
                }
            }
            
            return new MatchOdds(
                fixture.fixtureId(),
                fixture.participant1Name(),
                fixture.participant2Name(),
                fixture.startTime(),
                bookmakersList
            );
        } catch (IOException e) {
            System.err.println("Error calling OddsPapi odds API for fixture " + fixture.fixtureId() + ": " + e.getMessage());
            return null;
        }
    }
}

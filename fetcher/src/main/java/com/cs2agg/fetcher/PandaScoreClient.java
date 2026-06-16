package com.cs2agg.fetcher;

import com.cs2agg.fetcher.model.Match;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;

public class PandaScoreClient {
    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final String apiKey;

    public PandaScoreClient() {
        this.client = new OkHttpClient();
        this.mapper = new ObjectMapper();
        this.apiKey = System.getenv("PANDASCORE_API_KEY");
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            throw new IllegalStateException("PANDASCORE_API_KEY environment variable is not set");
        }
    }

    public List<Match> fetchUpcomingMatches() {
        String url = "https://api.pandascore.co/csgo/matches/upcoming" +
                "?filter[status]=not_started" +
                "&filter[tournament_tier]=s,a" +
                "&sort=begin_at" +
                "&per_page=50" +
                "&token=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new PandaScoreException("PandaScore API request failed with status: " + response.code() + " - " + response.message());
            }
            if (response.body() == null) {
                throw new PandaScoreException("PandaScore API response body was null");
            }
            String bodyString = response.body().string();
            return mapper.readValue(bodyString, new TypeReference<List<Match>>() {});
        } catch (IOException e) {
            throw new PandaScoreException("Failed to fetch matches from PandaScore API", e);
        }
    }
}

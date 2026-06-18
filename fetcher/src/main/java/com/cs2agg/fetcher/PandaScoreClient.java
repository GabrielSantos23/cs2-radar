package com.cs2agg.fetcher;

import com.cs2agg.fetcher.model.Match;
import com.cs2agg.fetcher.model.LiveMatch;
import com.cs2agg.fetcher.model.Bracket;
import com.cs2agg.fetcher.model.BracketMatch;
import com.cs2agg.fetcher.model.Team;
import com.cs2agg.fetcher.model.TournamentResult;
import com.cs2agg.fetcher.model.TeamPlacement;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.*;

public class PandaScoreClient {
    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final String apiKey;

    public PandaScoreClient() {
        this.client = new OkHttpClient();
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.apiKey = System.getenv("PANDASCORE_API_KEY");
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            throw new IllegalStateException("PANDASCORE_API_KEY environment variable is not set");
        }
    }

    public List<Match> fetchUpcomingMatches() {
        String url = "https://api.pandascore.co/csgo/matches/upcoming" +
                "?filter[status]=not_started" +
                "&sort=begin_at" +
                "&per_page=100" +
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
            List<Match> allMatches = mapper.readValue(bodyString, new TypeReference<List<Match>>() {});
            
            return allMatches;
        } catch (IOException e) {
            throw new PandaScoreException("Failed to fetch matches from PandaScore API", e);
        }
    }

    public List<LiveMatch> fetchLiveMatches() {
        String url = "https://api.pandascore.co/csgo/matches/running" +
                "?filter[tier]=s,a" +
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
            List<LiveMatch> allMatches = mapper.readValue(bodyString, new TypeReference<List<LiveMatch>>() {});
            
            return allMatches;
        } catch (IOException e) {
            throw new PandaScoreException("Failed to fetch live matches from PandaScore API", e);
        }
    }

    public List<Bracket> fetchBrackets(String tournamentId) {
        String url = "https://api.pandascore.co/csgo/tournaments/" + tournamentId + "/brackets?token=" + apiKey;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        String bodyString;
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) {
                String fallbackUrl = "https://api.pandascore.co/tournaments/" + tournamentId + "/brackets?token=" + apiKey;
                Request fallbackRequest = new Request.Builder()
                        .url(fallbackUrl)
                        .get()
                        .build();
                try (Response fallbackResponse = client.newCall(fallbackRequest).execute()) {
                    if (!fallbackResponse.isSuccessful()) {
                        throw new PandaScoreException("PandaScore API request failed with status: " + fallbackResponse.code() + " - " + fallbackResponse.message());
                    }
                    if (fallbackResponse.body() == null) {
                        throw new PandaScoreException("PandaScore API response body was null");
                    }
                    bodyString = fallbackResponse.body().string();
                }
            } else {
                if (!response.isSuccessful()) {
                    throw new PandaScoreException("PandaScore API request failed with status: " + response.code() + " - " + response.message());
                }
                if (response.body() == null) {
                    throw new PandaScoreException("PandaScore API response body was null");
                }
                bodyString = response.body().string();
            }
        } catch (IOException e) {
            throw new PandaScoreException("Failed to fetch brackets from PandaScore API", e);
        }

        try {
            List<RawMatch> rawMatches = mapper.readValue(bodyString, new TypeReference<List<RawMatch>>() {});
            if (rawMatches == null || rawMatches.isEmpty()) {
                return List.of();
            }

            Map<String, RawMatch> matchMap = new HashMap<>();
            for (RawMatch m : rawMatches) {
                if (m.id() != null) {
                    matchMap.put(m.id(), m);
                }
            }

            Map<String, Integer> depthCache = new HashMap<>();
            Map<Integer, List<RawMatch>> matchesByDepth = new HashMap<>();

            for (RawMatch m : rawMatches) {
                int depth = getDepth(m, matchMap, depthCache, new HashSet<>());
                matchesByDepth.computeIfAbsent(depth, k -> new ArrayList<>()).add(m);
            }

            List<Bracket> brackets = new ArrayList<>();
            for (Map.Entry<Integer, List<RawMatch>> entry : matchesByDepth.entrySet()) {
                int depth = entry.getKey();
                int roundNumber = depth + 1;
                List<RawMatch> roundRawMatches = entry.getValue();

                roundRawMatches.sort(Comparator.comparing((RawMatch m) -> m.scheduledAt() != null ? m.scheduledAt() : "")
                        .thenComparing(RawMatch::id));

                List<BracketMatch> bracketMatches = new ArrayList<>();
                for (RawMatch rm : roundRawMatches) {
                    Team team1 = null;
                    Team team2 = null;
                    if (rm.opponents() != null) {
                        if (rm.opponents().size() > 0 && rm.opponents().get(0).opponent() != null) {
                            team1 = rm.opponents().get(0).opponent();
                        }
                        if (rm.opponents().size() > 1 && rm.opponents().get(1).opponent() != null) {
                            team2 = rm.opponents().get(1).opponent();
                        }
                    }

                    Integer score1 = 0;
                    Integer score2 = 0;
                    if (rm.results() != null) {
                        for (Result r : rm.results()) {
                            if (team1 != null && team1.id() != null && team1.id().equals(r.teamId())) {
                                score1 = r.score();
                            }
                            if (team2 != null && team2.id() != null && team2.id().equals(r.teamId())) {
                                score2 = r.score();
                            }
                        }
                    }

                    bracketMatches.add(new BracketMatch(
                            rm.id(),
                            team1,
                            team2,
                            score1,
                            score2,
                            rm.status(),
                            rm.scheduledAt()
                    ));
                }

                String roundName = determineRoundName(roundNumber, roundRawMatches);
                brackets.add(new Bracket(roundName, roundNumber, bracketMatches));
            }

            brackets.sort(Comparator.comparingInt(Bracket::roundNumber));
            return brackets;
        } catch (IOException e) {
            throw new PandaScoreException("Failed to parse brackets JSON", e);
        }
    }

    private int getDepth(RawMatch match, Map<String, RawMatch> matchMap, Map<String, Integer> depthCache, Set<String> visiting) {
        if (depthCache.containsKey(match.id())) {
            return depthCache.get(match.id());
        }
        if (visiting.contains(match.id())) {
            return 0;
        }

        visiting.add(match.id());
        int maxPrevDepth = -1;
        if (match.previousMatches() != null) {
            for (PrevMatch pm : match.previousMatches()) {
                RawMatch prevMatch = matchMap.get(pm.matchId());
                if (prevMatch != null) {
                    maxPrevDepth = Math.max(maxPrevDepth, getDepth(prevMatch, matchMap, depthCache, visiting));
                }
            }
        }
        visiting.remove(match.id());

        int depth = maxPrevDepth + 1;
        depthCache.put(match.id(), depth);
        return depth;
    }

    private String determineRoundName(int roundNumber, List<RawMatch> matches) {
        boolean hasGrandFinal = false;
        boolean hasSemi = false;
        boolean hasQuarter = false;
        boolean hasDecider = false;
        boolean hasElimination = false;
        boolean hasWinners = false;
        boolean hasOpening = false;

        for (RawMatch m : matches) {
            if (m.name() != null) {
                String nameLower = m.name().toLowerCase();
                if (nameLower.contains("grand final")) {
                    hasGrandFinal = true;
                } else if (nameLower.contains("semi")) {
                    hasSemi = true;
                } else if (nameLower.contains("quarter")) {
                    hasQuarter = true;
                } else if (nameLower.contains("decider")) {
                    hasDecider = true;
                } else if (nameLower.contains("elimination")) {
                    hasElimination = true;
                } else if (nameLower.contains("winners")) {
                    hasWinners = true;
                } else if (nameLower.contains("opening") || nameLower.contains("match 1") || nameLower.contains("match 2")) {
                    hasOpening = true;
                }
            }
        }

        if (hasGrandFinal) return "Grand Final";
        if (hasSemi) return "Semifinals";
        if (hasQuarter) return "Quarterfinals";
        if (hasDecider) return "Decider Matches";
        if (hasElimination) return "Elimination Matches";
        if (hasWinners) return "Winners Matches";
        if (hasOpening) return "Opening Matches";

        return "Round " + roundNumber;
    }

    private record RawMatch(
        String id,
        String name,
        String status,
        @JsonProperty("scheduled_at") String scheduledAt,
        List<OpponentWrapper> opponents,
        List<Result> results,
        @JsonProperty("previous_matches") List<PrevMatch> previousMatches
    ) {}

    private record OpponentWrapper(
        String type,
        Team opponent
    ) {}

    private record Result(
        @JsonProperty("team_id") String teamId,
        int score
    ) {}

    private record PrevMatch(
        String type,
        @JsonProperty("match_id") String matchId
    ) {}

    public List<TournamentResult> fetchRecentTournamentResults() {
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant sixMonthsAgo = now.minus(180, java.time.temporal.ChronoUnit.DAYS);
        String dateRange = sixMonthsAgo.toString() + "," + now.toString();

        String url = "https://api.pandascore.co/csgo/tournaments/past" +
                "?filter[tier]=s,a" +
                "&range[end_at]=" + dateRange +
                "&sort=-end_at" +
                "&per_page=50" +
                "&token=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                System.err.println("Failed to fetch past tournaments: " + response.code());
                return List.of();
            }
            String bodyString = response.body().string();
            JsonNode tournamentsNode = mapper.readTree(bodyString);
            List<TournamentResult> results = new ArrayList<>();

            if (tournamentsNode.isArray()) {
                for (JsonNode tNode : tournamentsNode) {
                    String tournamentId = tNode.get("id").asText();
                    String tournamentName = tNode.get("name").asText();
                    String tier = tNode.get("tier").asText();
                    String endAt = tNode.has("end_at") && !tNode.get("end_at").isNull() ? tNode.get("end_at").asText() : "";

                    List<TeamPlacement> placements = fetchTournamentPlacements(tournamentId);
                    results.add(new TournamentResult(tournamentId, tournamentName, tier, endAt, placements));
                }
            }
            return results;
        } catch (IOException e) {
            throw new PandaScoreException("Failed to fetch recent tournament results", e);
        }
    }

    private List<TeamPlacement> fetchTournamentPlacements(String tournamentId) {
        String url = "https://api.pandascore.co/tournaments/" + tournamentId + "/standings?token=" + apiKey;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                System.err.println("Failed to fetch standings for tournament ID " + tournamentId + ": " + response.code());
                return List.of();
            }
            String bodyString = response.body().string();
            JsonNode standingsNode = mapper.readTree(bodyString);
            List<TeamPlacement> placements = new ArrayList<>();

            if (standingsNode.isArray()) {
                for (JsonNode sNode : standingsNode) {
                    int position = sNode.has("rank") && !sNode.get("rank").isNull() ? sNode.get("rank").asInt() : 1;
                    if (sNode.has("team") && !sNode.get("team").isNull()) {
                        JsonNode teamNode = sNode.get("team");
                        String teamId = teamNode.get("id").asText();
                        String teamName = teamNode.get("name").asText();
                        String imageUrl = teamNode.has("image_url") && !teamNode.get("image_url").isNull() 
                                ? teamNode.get("image_url").asText() : "";
                        placements.add(new TeamPlacement(teamId, teamName, imageUrl, position));
                    }
                }
            }
            return placements;
        } catch (IOException e) {
            System.err.println("Error fetching standings for tournament " + tournamentId + ": " + e.getMessage());
            return List.of();
        }
    }
}

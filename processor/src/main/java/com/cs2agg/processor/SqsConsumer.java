package com.cs2agg.processor;

import com.cs2agg.processor.model.Bracket;
import com.cs2agg.processor.model.MatchEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cs2agg.processor.model.TournamentResult;
import com.cs2agg.processor.model.TeamRankEntry;
import com.cs2agg.processor.model.LiveMatchEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Component
public class SqsConsumer {
    private final MatchRepository matchRepository;
    private final BracketRepository bracketRepository;
    private final OddsCache oddsCache;
    private final RankingRepository rankingRepository;
    private final RankingCalculator rankingCalculator;
    private final LiveMatchRepository liveMatchRepository;
    private final ObjectMapper mapper;

    public SqsConsumer(MatchRepository matchRepository, 
                       BracketRepository bracketRepository, 
                       OddsCache oddsCache,
                       RankingRepository rankingRepository,
                       RankingCalculator rankingCalculator,
                       LiveMatchRepository liveMatchRepository) {
        this.matchRepository = matchRepository;
        this.bracketRepository = bracketRepository;
        this.oddsCache = oddsCache;
        this.rankingRepository = rankingRepository;
        this.rankingCalculator = rankingCalculator;
        this.liveMatchRepository = liveMatchRepository;
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @SqsListener("${app.sqs.queue-url}")
    public void listen(String messageBody) {
        try {
            JsonNode node = mapper.readTree(messageBody);
            if (node.has("type") && "bracket".equals(node.get("type").asText())) {
                String tournamentId = node.get("tournamentId").asText();
                System.out.println("Received SQS bracket event for tournament ID=" + tournamentId);
                List<Bracket> brackets = mapper.readValue(
                        node.get("brackets").toString(),
                        new TypeReference<List<Bracket>>() {}
                );
                for (Bracket bracket : brackets) {
                    bracketRepository.save(tournamentId, bracket);
                }
                System.out.println("Successfully processed bracket event for tournament ID=" + tournamentId);
            } else if (node.has("type") && "tournament_result".equals(node.get("type").asText())) {
                String tournamentId = node.get("tournamentId").asText();
                System.out.println("Received SQS tournament result event for tournament ID=" + tournamentId);
                TournamentResult result = mapper.readValue(messageBody, TournamentResult.class);
                
                rankingRepository.saveRawPlacements(result);
                
                List<TournamentResult> allResults = rankingRepository.loadAllRawPlacements();
                List<TeamRankEntry> currentRanking = rankingCalculator.computeRanking(allResults);

                String today = java.time.LocalDate.now().toString();
                String sevenDaysAgo = java.time.LocalDate.now().minusDays(7).toString();
                List<TeamRankEntry> previousRanking = rankingRepository.getSnapshot(sevenDaysAgo);

                Map<String, Integer> prevPosMap = new HashMap<>();
                for (TeamRankEntry prev : previousRanking) {
                    prevPosMap.put(prev.teamId(), prev.position());
                }

                List<TeamRankEntry> finalizedRanking = new java.util.ArrayList<>();
                for (TeamRankEntry curr : currentRanking) {
                    int prevPos = prevPosMap.getOrDefault(curr.teamId(), 0);
                    finalizedRanking.add(new TeamRankEntry(
                        curr.position(),
                        prevPos,
                        curr.teamId(),
                        curr.teamName(),
                        curr.imageUrl(),
                        curr.score()
                    ));
                }

                rankingRepository.saveSnapshot(today, finalizedRanking);
                System.out.println("Successfully calculated and saved CS2 ranking snapshot for date: " + today);
            } else if (node.has("type") && "live_match".equals(node.get("type").asText())) {
                System.out.println("Received SQS live match event");
                LiveMatchEvent event = mapper.readValue(messageBody, LiveMatchEvent.class);
                liveMatchRepository.save(event);
                System.out.println("Successfully processed live match event: ID=" + event.id());
            } else {
                MatchEvent event = mapper.readValue(messageBody, MatchEvent.class);
                System.out.println("Received SQS match event: ID=" + event.id() + ", Name=" + event.name());
                matchRepository.save(event);
                if (event.odds() != null) {
                    oddsCache.cacheOdds(event.id(), event.odds());
                }
                System.out.println("Successfully processed match event: ID=" + event.id());
            }
        } catch (Exception e) {
            System.err.println("Error processing SQS message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

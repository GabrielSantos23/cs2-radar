package com.cs2agg.processor;

import com.cs2agg.processor.model.Bracket;
import com.cs2agg.processor.model.MatchEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SqsConsumer {
    private final MatchRepository matchRepository;
    private final BracketRepository bracketRepository;
    private final OddsCache oddsCache;
    private final ObjectMapper mapper;

    public SqsConsumer(MatchRepository matchRepository, BracketRepository bracketRepository, OddsCache oddsCache) {
        this.matchRepository = matchRepository;
        this.bracketRepository = bracketRepository;
        this.oddsCache = oddsCache;
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

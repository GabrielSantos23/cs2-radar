package com.cs2agg.processor;

import com.cs2agg.processor.model.MatchEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

@Component
public class SqsConsumer {
    private final MatchRepository matchRepository;
    private final OddsCache oddsCache;

    public SqsConsumer(MatchRepository matchRepository, OddsCache oddsCache) {
        this.matchRepository = matchRepository;
        this.oddsCache = oddsCache;
    }

    @SqsListener("${app.sqs.queue-url}")
    public void listen(MatchEvent event) {
        System.out.println("Received SQS match event: ID=" + event.id() + ", Name=" + event.name());
        try {
            matchRepository.save(event);
            if (event.odds() != null) {
                oddsCache.cacheOdds(event.id(), event.odds());
            }
            System.out.println("Successfully processed match event: ID=" + event.id());
        } catch (Exception e) {
            System.err.println("Error processing match event ID " + event.id() + ": " + e.getMessage());
            throw e;
        }
    }
}

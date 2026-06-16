package com.cs2agg.fetcher;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.cs2agg.fetcher.model.Match;

import java.util.List;

public class FetcherHandler implements RequestHandler<ScheduledEvent, Void> {
    private final PandaScoreClient client;
    private final SqsPublisher publisher;

    public FetcherHandler() {
        this.client = new PandaScoreClient();
        this.publisher = new SqsPublisher();
    }

    @Override
    public Void handleRequest(ScheduledEvent event, Context context) {
        System.out.println("Fetcher Lambda invoked. Event ID: " + event.getId() + ", Time: " + event.getTime());
        try {
            List<Match> matches = client.fetchUpcomingMatches();
            System.out.println("Fetched " + matches.size() + " matches from PandaScore.");
            
            publisher.publish(matches);
            System.out.println("Published matches successfully to SQS.");

            // Passo 1: Extrair torneios ativos e coletar brackets
            List<String> tournamentIds = matches.stream()
                    .map(Match::tournamentId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            System.out.println("Found " + tournamentIds.size() + " unique active tournaments. Fetching brackets...");
            for (String tournamentId : tournamentIds) {
                try {
                    List<com.cs2agg.fetcher.model.Bracket> brackets = client.fetchBrackets(tournamentId);
                    if (!brackets.isEmpty()) {
                        publisher.publishBracket(tournamentId, brackets);
                    } else {
                        System.out.println("No brackets found for tournament: " + tournamentId);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to fetch/publish brackets for tournament ID " + tournamentId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in FetcherHandler: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return null;
    }
}

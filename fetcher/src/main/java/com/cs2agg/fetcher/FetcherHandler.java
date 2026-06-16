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
        } catch (Exception e) {
            System.err.println("Error in FetcherHandler: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return null;
    }
}

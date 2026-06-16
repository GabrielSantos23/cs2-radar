package com.cs2agg.fetcher;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

public class LocalFetcherRunner {
    public static void main(String[] args) {
        System.out.println("Starting Local Fetcher Runner...");
        try {
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

package com.cs2agg.fetcher;

import com.cs2agg.fetcher.model.Match;
import com.cs2agg.fetcher.model.LiveMatch;
import com.cs2agg.fetcher.model.Bracket;
import com.cs2agg.fetcher.model.BracketMessage;
import com.cs2agg.fetcher.model.TournamentResult;
import com.cs2agg.fetcher.model.TeamPlacement;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SqsPublisher {
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper mapper;

    public SqsPublisher() {
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        var builder = SqsClient.builder();
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            builder.endpointOverride(java.net.URI.create(endpoint));
        }
        this.sqsClient = builder.build();
        this.queueUrl = System.getenv("SQS_QUEUE_URL");
        this.mapper = new ObjectMapper();
        if (this.queueUrl == null || this.queueUrl.trim().isEmpty()) {
            throw new IllegalStateException("SQS_QUEUE_URL environment variable is not set");
        }
    }

    public void publish(List<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            return;
        }

        List<SendMessageBatchRequestEntry> entries = new ArrayList<>();
        for (Match match : matches) {
            try {
                String jsonMessage = mapper.writeValueAsString(match);
                SendMessageBatchRequestEntry entry = SendMessageBatchRequestEntry.builder()
                        .id(UUID.randomUUID().toString())
                        .messageBody(jsonMessage)
                        .build();
                entries.add(entry);

                if (entries.size() == 10) {
                    sendBatch(entries);
                    entries.clear();
                }
            } catch (Exception e) {
                System.err.println("Failed to serialize match ID " + match.id() + ": " + e.getMessage());
            }
        }

        if (!entries.isEmpty()) {
            sendBatch(entries);
        }
    }

    private void sendBatch(List<SendMessageBatchRequestEntry> entries) {
        SendMessageBatchRequest batchRequest = SendMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(entries)
                .build();
        SendMessageBatchResponse response = sqsClient.sendMessageBatch(batchRequest);
        if (response.hasFailed() && !response.failed().isEmpty()) {
            System.err.println("Failed to send some messages in SQS batch: " + response.failed().size() + " errors");
            response.failed().forEach(err -> 
                System.err.println("Error details - ID: " + err.id() + ", Code: " + err.code() + ", Message: " + err.message())
            );
        }
    }

    public void publishBracket(String tournamentId, List<Bracket> brackets) {
        if (brackets == null || brackets.isEmpty()) {
            return;
        }
        try {
            BracketMessage msg = new BracketMessage("bracket", tournamentId, brackets);
            String jsonMessage = mapper.writeValueAsString(msg);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(jsonMessage)
                    .build());
            System.out.println("Published bracket for tournament ID " + tournamentId + " successfully to SQS.");
        } catch (Exception e) {
            System.err.println("Failed to serialize or publish bracket for tournament ID " + tournamentId + ": " + e.getMessage());
        }
    }

    public void publishTournamentResults(List<TournamentResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        for (TournamentResult res : results) {
            try {
                java.util.Map<String, Object> msg = new java.util.HashMap<>();
                msg.put("type", "tournament_result");
                msg.put("tournamentId", res.tournamentId());
                msg.put("tournamentName", res.tournamentName());
                msg.put("tier", res.tier());
                msg.put("endAt", res.endAt());
                msg.put("placements", res.placements());

                String jsonMessage = mapper.writeValueAsString(msg);
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(jsonMessage)
                        .build());
                System.out.println("Published tournament result for tournament ID " + res.tournamentId() + " successfully to SQS.");
            } catch (Exception e) {
                System.err.println("Failed to serialize or publish tournament result for ID " + res.tournamentId() + ": " + e.getMessage());
            }
        }
    }

    public void publishLiveMatches(List<LiveMatch> liveMatches) {
        if (liveMatches == null || liveMatches.isEmpty()) {
            return;
        }
        for (LiveMatch lm : liveMatches) {
            try {
                java.util.Map<String, Object> msg = new java.util.HashMap<>();
                msg.put("type", "live_match");
                msg.put("id", lm.id());
                msg.put("status", lm.status());
                msg.put("team1", lm.team1());
                msg.put("team2", lm.team2());
                msg.put("score1", lm.score1());
                msg.put("score2", lm.score2());
                msg.put("currentGame", lm.currentGame());
                msg.put("tournamentId", lm.tournamentId());
                msg.put("tournamentName", lm.tournamentName());

                String jsonMessage = mapper.writeValueAsString(msg);
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(jsonMessage)
                        .build());
                System.out.println("Published live match ID " + lm.id() + " successfully to SQS.");
            } catch (Exception e) {
                System.err.println("Failed to serialize or publish live match ID " + lm.id() + ": " + e.getMessage());
            }
        }
    }
}

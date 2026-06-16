package com.cs2agg.fetcher;

import com.cs2agg.fetcher.model.Match;
import com.cs2agg.fetcher.model.Bracket;
import com.cs2agg.fetcher.model.BracketMessage;
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
}

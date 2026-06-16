#!/bin/bash
export AWS_ACCESS_KEY_ID=mock
export AWS_SECRET_ACCESS_KEY=mock
export AWS_DEFAULT_REGION=us-east-1
ENDPOINT="http://localhost:4566"

echo "Creating SQS queue: cs2-match-queue..."
aws sqs create-queue --queue-name cs2-match-queue --endpoint-url $ENDPOINT

echo "Creating DynamoDB table: cs2-matches..."
aws dynamodb create-table \
  --table-name cs2-matches \
  --attribute-definitions \
    AttributeName=tournamentId,AttributeType=S \
    AttributeName=matchId,AttributeType=S \
  --key-schema \
    AttributeName=tournamentId,KeyType=HASH \
    AttributeName=matchId,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url $ENDPOINT

echo "Local resources initialized successfully!"

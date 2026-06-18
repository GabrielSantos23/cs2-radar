$env:AWS_ACCESS_KEY_ID = "mock"
$env:AWS_SECRET_ACCESS_KEY = "mock"
$env:AWS_DEFAULT_REGION = "us-east-1"
$ENDPOINT = "http://localhost:4566"

Write-Host "Creating SQS queue: cs2-match-queue..."
aws sqs create-queue --queue-name cs2-match-queue --endpoint-url $ENDPOINT

Write-Host "Creating DynamoDB table: cs2-matches..."
aws dynamodb create-table `
  --table-name cs2-matches `
  --attribute-definitions AttributeName=tournamentId,AttributeType=S AttributeName=matchId,AttributeType=S `
  --key-schema AttributeName=tournamentId,KeyType=HASH AttributeName=matchId,KeyType=RANGE `
  --billing-mode PAY_PER_REQUEST `
  --endpoint-url $ENDPOINT

Write-Host "Creating DynamoDB table: cs2-ranking..."
aws dynamodb create-table `
  --table-name cs2-ranking `
  --attribute-definitions AttributeName=snapshotDate,AttributeType=S AttributeName=teamId,AttributeType=S `
  --key-schema AttributeName=snapshotDate,KeyType=HASH AttributeName=teamId,KeyType=RANGE `
  --billing-mode PAY_PER_REQUEST `
  --endpoint-url $ENDPOINT

Write-Host "Local resources initialized successfully!"

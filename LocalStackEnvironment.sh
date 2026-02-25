#!/bin/bash

aws dynamodb create-table \
  --endpoint-url=http://localhost:4566 \
  --table-name ticketing-table-dev \
  --attribute-definitions \
      AttributeName=pk,AttributeType=S \
      AttributeName=sk,AttributeType=S \
      AttributeName=type,AttributeType=S \
  --key-schema \
      AttributeName=pk,KeyType=HASH \
      AttributeName=sk,KeyType=RANGE \
  --global-secondary-indexes \
        "[{\"IndexName\": \"EntityTypeIndex\",\"KeySchema\":[{\"AttributeName\":\"type\",\"KeyType\":\"HASH\"}],\"Projection\":{\"ProjectionType\":\"ALL\"}}]" \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1

aws sqs create-queue \
    --endpoint-url=http://localhost:4566 \
    --queue-name purchase-queue-dlq-dev.fifo \
    --attributes FifoQueue=true \
    --region us-east-1

aws sqs create-queue \
    --endpoint-url=http://localhost:4566 \
    --queue-name purchase-queue-dev.fifo \
    --attributes '{
      "FifoQueue": "true",
      "ContentBasedDeduplication": "true",
      "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:purchase-queue-dlq-dev.fifo\",\"maxReceiveCount\":\"3\"}"
    }' \
    --region us-east-1

aws sqs create-queue \
    --endpoint-url=http://localhost:4566 \
    --queue-name release-tickets-queue-dev \
    --region us-east-1
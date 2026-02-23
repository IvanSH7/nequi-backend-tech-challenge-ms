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

aws --endpoint-url=http://localhost:4566 dynamodb list-tables \
    --region us-east-1

aws --endpoint-url=http://localhost:4566 dynamodb scan \
    --table-name ticketing-table-dev \
    --region us-east-1

aws --endpoint-url=http://localhost:4566 dynamodb delete-table \
    --table-name ticketing-table-dev \
    --region us-east-1

aws --endpoint-url=http://localhost:4566 dynamodb scan \
  --table-name ticketing-table-dev \
  --filter-expression "#n = :value" \
  --expression-attribute-names '{"#n":"name"}' \
  --expression-attribute-values '{":value":{"S":"Zonas5"}}'

aws sqs receive-message \
  --endpoint-url http://localhost:4566 \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/purchase-queue-dev.fifo \
  --max-number-of-messages 10 \
  --region us-east-1

aws sqs purge-queue \
    --endpoint-url=http://localhost:4566 \
    --queue-url http://localhost:4566/000000000000/purchase-queue-dev.fifo \
    --region us-east-1

aws dynamodb query \
  --endpoint-url=http://localhost:4566 \
  --table-name ticketing-table-dev \
  --key-condition-expression "#n = :value" \
  --expression-attribute-names '{"#n":"name"}' \
  --expression-attribute-values '{":value":{"S":"Zonas5"}}'

aws dynamodb get-item \
    --endpoint-url=http://localhost:4566 \
    --table-name ticketing-table-dev \
    --key '{"pk": {"S": "EVENT#fe38c9bb-638c-45be-ad44-408776ae58a0"}, "sk": {"S": "METADATA"}}' \
    --region us-east-1

aws dynamodb get-item \
    --endpoint-url=http://localhost:4566 \
    --table-name ticketing-table-dev \
    --key '{"pk": {"S": "ORDER#702c5234-321b-4a0d-8b9a-30587f2f9ee0"}, "sk": {"S": "METADATA"}}' \
    --region us-east-1

aws dynamodb query \
    --endpoint-url=http://localhost:4566 \
    --table-name ticketing-table-dev \
    --key-condition-expression "pk = :pk AND begins_with(sk, :skPrefix)" \
    --expression-attribute-values '{":pk": {"S": "EVENT#fe38c9bb-638c-45be-ad44-408776ae58a0"}, ":skPrefix": {"S": "TICKET#"}}' \
    --region us-east-1
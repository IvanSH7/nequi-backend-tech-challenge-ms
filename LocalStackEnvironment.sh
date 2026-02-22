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
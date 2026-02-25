package com.nequi.dynamodb.repositories;

import com.nequi.dynamodb.dto.EventDto;
import com.nequi.dynamodb.entities.EventEntity;
import com.nequi.dynamodb.helper.TemplateAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

@Slf4j
@Repository
public class EventDynamoRepository extends TemplateAdapterOperations<EventDto, String, EventEntity> {

    public EventDynamoRepository(DynamoDbEnhancedAsyncClient connectionFactory, ObjectMapper mapper,
                                 @Value("${adapter.aws.dynamo-db.ticketing-table-name}") String tableName) {
        super(connectionFactory, mapper, d -> mapper.map(d, EventDto.class), tableName);
    }

}

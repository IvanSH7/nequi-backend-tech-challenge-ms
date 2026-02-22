package com.nequi.dynamodb.repositories;

import com.nequi.dynamodb.dto.TicketDto;
import com.nequi.dynamodb.entities.TicketEntity;
import com.nequi.dynamodb.helper.TemplateAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

@Slf4j
@Repository
public class TicketDynamoRepository extends TemplateAdapterOperations<TicketDto, String, TicketEntity> {

    public TicketDynamoRepository(DynamoDbEnhancedAsyncClient connectionFactory, ObjectMapper mapper,
                                  @Value("${adapter.aws.dynamo-db.ticketing-table-name}") String tableName) {
        super(connectionFactory, mapper, d -> mapper.map(d, TicketDto.class), tableName);
    }

}

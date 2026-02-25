package com.nequi.dynamodb.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;


import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@DynamoDbBean
public class TicketEntity {

    @Getter(onMethod_ = {
            @DynamoDbPartitionKey,
            @DynamoDbAttribute("pk")
    })
    private String pk;
    @Getter(onMethod_ = {
            @DynamoDbSortKey,
            @DynamoDbAttribute("sk")
    })
    private String sk;
    @Getter(onMethod_ = {@DynamoDbAttribute("type")})
    private String type;
    @Getter(onMethod_ = {@DynamoDbAttribute("status")})
    private String status;
    @Getter(onMethod_ = {@DynamoDbAttribute("version"), @DynamoDbVersionAttribute})
    private Long version;
    @Getter(onMethod_ = {@DynamoDbAttribute("orderId"), @DynamoDbIgnoreNulls})
    @JsonInclude(NON_NULL)
    private String orderId;

}

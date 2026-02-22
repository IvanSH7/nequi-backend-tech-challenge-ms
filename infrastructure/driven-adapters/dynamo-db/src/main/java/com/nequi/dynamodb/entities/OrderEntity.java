package com.nequi.dynamodb.entities;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@DynamoDbBean
public class OrderEntity {

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
    @Getter(onMethod_ = {@DynamoDbAttribute("eventId")})
    private String eventId;
    @Getter(onMethod_ = {@DynamoDbAttribute("quantity")})
    private Integer quantity;
    @Getter(onMethod_ = {@DynamoDbAttribute("expiresAt")})
    private Long expiresAt;

}

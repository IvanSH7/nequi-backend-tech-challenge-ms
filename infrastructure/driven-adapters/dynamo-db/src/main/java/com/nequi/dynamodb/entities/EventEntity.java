package com.nequi.dynamodb.entities;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;


@Data
@DynamoDbBean
public class EventEntity {

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
    @Getter(onMethod_ = {
            @DynamoDbSecondaryPartitionKey(indexNames = "EntityTypeIndex"),
            @DynamoDbAttribute("type"),
            @DynamoDbIgnoreNulls})
    private String type;
    @Getter(onMethod_ = {@DynamoDbAttribute("status")})
    private String status;
    @Getter(onMethod_ = {@DynamoDbAttribute("totalCapacity"), @DynamoDbIgnoreNulls})
    private Long totalCapacity;
    @Getter(onMethod_ = {@DynamoDbAttribute("availableCount"), @DynamoDbIgnoreNulls})
    private Long availableCount;
    @Getter(onMethod_ = {@DynamoDbAttribute("name"), @DynamoDbIgnoreNulls})
    private String name;
    @Getter(onMethod_ = {@DynamoDbAttribute("place"), @DynamoDbIgnoreNulls})
    private String place;
    @Getter(onMethod_ = {@DynamoDbAttribute("date"), @DynamoDbIgnoreNulls})
    private String date;

}

package com.nequi.dynamodb.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String TYPE_EVENT = "Event";
    public static final String TYPE_TICKET = "Ticket";
    public static final String TYPE_ORDER = "Order";
    public static final String SORT_KEY_METADATA = "METADATA";
    public static final String BASE_EVENT_PK = "EVENT#";
    public static final String BASE_ORDER_PK = "ORDER#";
    public static final String BASE_TICKET_PK = "TICKET#";
    public static final String ORDER_ID = "orderId";
    public static final String TYPE_INDEX = "EntityTypeIndex";

}
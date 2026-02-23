package com.nequi.sqs.listener.mapper;

import com.nequi.model.order.Order;
import com.nequi.sqs.listener.dto.ProcessOrderMessage;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;


@Mapper(imports = { LocalDateTime.class })
public interface SqsMapper {

    SqsMapper MAPPER = Mappers.getMapper(SqsMapper.class);

    Order toDomain(ProcessOrderMessage processOrderMessage);

}

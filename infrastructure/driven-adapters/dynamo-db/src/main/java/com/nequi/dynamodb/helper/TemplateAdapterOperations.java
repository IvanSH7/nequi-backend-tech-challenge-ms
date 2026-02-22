package com.nequi.dynamodb.helper;

import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.function.Function;

public abstract class TemplateAdapterOperations<E, K, V> {
    private final Class<V> dataClass;
    private final Function<V, E> toEntityFn;
    protected ObjectMapper mapper;
    private final DynamoDbAsyncTable<V> table;
    private final DynamoDbAsyncIndex<V> tableByIndex;

    private final DynamoDbEnhancedAsyncClient enhancedClient;

    @SuppressWarnings("unchecked")
    protected TemplateAdapterOperations(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                        ObjectMapper mapper,
                                        Function<V, E> toEntityFn,
                                        String tableName,
                                        String... index) {
        this.toEntityFn = toEntityFn;
        this.mapper = mapper;
        this.enhancedClient = dynamoDbEnhancedAsyncClient;
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        this.dataClass = (Class<V>) genericSuperclass.getActualTypeArguments()[2];
        table = dynamoDbEnhancedAsyncClient.table(tableName, TableSchema.fromBean(dataClass));
        tableByIndex = index.length > 0 ? table.index(index[0]) : null;
    }

    public Mono<E> save(E model) {
        return Mono.fromFuture(table.putItem(toEntity(model))).thenReturn(model);
    }

    public Mono<List<E>> processBatch(List<E> batchModels) {
        WriteBatch.Builder<V> writeBatchBuilder = WriteBatch.builder(dataClass)
                .mappedTableResource(table);

        for (E model : batchModels) {
            writeBatchBuilder.addPutItem(b -> b.item(toEntity(model)));
        }

        BatchWriteItemEnhancedRequest batchRequest = BatchWriteItemEnhancedRequest.builder()
                .addWriteBatch(writeBatchBuilder.build())
                .build();

        return Mono.fromFuture(enhancedClient.batchWriteItem(batchRequest))
                .thenReturn(batchModels);
    }

    public Mono<E> update(E model) {
        UpdateItemEnhancedRequest<V> updateRequest = UpdateItemEnhancedRequest.builder(dataClass)
                .item(toEntity(model))
                .ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY)
                .build();
        return Mono.fromFuture(table.updateItem(updateRequest)).map(this::toModel);
    }

    public Mono<E> getById(K id) {
        return Mono.fromFuture(table.getItem(Key.builder()
                        .partitionValue(AttributeValue.builder().s((String) id).build())
                        .build()))
                .map(this::toModel);
    }

    public Mono<E> getById(K partitionKey, K sortKey) {
        return Mono.fromFuture(table.getItem(Key.builder()
                        .partitionValue(AttributeValue.builder().s((String) partitionKey).build())
                        .sortValue(AttributeValue.builder().s((String) sortKey).build())
                        .build()))
                .map(this::toModel);
    }

    public Mono<E> delete(E model) {
        return Mono.fromFuture(table.deleteItem(toEntity(model))).map(this::toModel);
    }

    public Mono<List<E>> query(QueryEnhancedRequest queryExpression) {
        PagePublisher<V> pagePublisher = table.query(queryExpression);
        return listOfModel(pagePublisher);
    }

    public Mono<List<E>> queryByIndex(QueryEnhancedRequest queryExpression, String... index) {
        DynamoDbAsyncIndex<V> queryIndex = index.length > 0 ? table.index(index[0]) : tableByIndex;

        SdkPublisher<Page<V>> pagePublisher = queryIndex.query(queryExpression);
        return listOfModel(pagePublisher);
    }


    private Mono<List<E>> listOfModel(PagePublisher<V> pagePublisher) {
        return Mono.from(pagePublisher).map(page -> page.items().stream().map(this::toModel).toList());
    }

    private Mono<List<E>> listOfModel(SdkPublisher<Page<V>> pagePublisher) {
        return Mono.from(pagePublisher).map(page -> page.items().stream().map(this::toModel).toList());
    }

    protected V toEntity(E model) {
        return mapper.map(model, dataClass);
    }

    protected E toModel(V data) {
        return data != null ? toEntityFn.apply(data) : null;
    }
}
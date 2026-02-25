package com.nequi.sqs.sender.config;

import com.nequi.model.order.gateways.ProcessorGateway;
import com.nequi.sqs.sender.SQSSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

@Configuration
public class SQSSenderConfig {

    @Value("${adapter.aws.region}")
    private String region;
    @Value("${adapter.aws.sqs.endpoint}")
    private String endpoint;

    @Bean("senderClient")
    @Profile({"local"})
    public SqsAsyncClient sqsClient() {
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
    }

    @Bean("senderClient")
    @Profile({"dev", "qa", "pdn"})
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(getProviderChain())
                .build();
    }

    private AwsCredentialsProviderChain getProviderChain() {
        return AwsCredentialsProviderChain.builder()
                .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .addCredentialsProvider(SystemPropertyCredentialsProvider.create())
                .addCredentialsProvider(WebIdentityTokenFileCredentialsProvider.create())
                .addCredentialsProvider(ProfileCredentialsProvider.create())
                .addCredentialsProvider(ContainerCredentialsProvider.builder().build())
                .addCredentialsProvider(InstanceProfileCredentialsProvider.create())
                .build();
    }

    @Bean
    public ProcessorGateway processorGateway(@Qualifier("senderClient") SqsAsyncClient sqsAsyncClient,
                                             @Value("${adapter.aws.sqs.purchase.queue-url}") String purchaseQueueUrl,
                                             @Value("${adapter.aws.sqs.release.queue-url}") String releaseQueueUrl,
                                             ObjectMapper objectMapper) {
        return new SQSSender(sqsAsyncClient, purchaseQueueUrl, releaseQueueUrl, objectMapper);
    }

}

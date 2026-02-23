package com.nequi.sqs.listener.config;

import com.nequi.sqs.listener.helper.FifoListener;
import com.nequi.sqs.listener.helper.StandardListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.net.URI;
import java.util.function.Function;

@Configuration
public class SQSConfig {

    @Value("${adapter.aws.region}")
    private String region;
    @Value("${adapter.aws.sqs.endpoint}")
    private String endpoint;

    @Bean("listenerClient")
    @Profile({"local"})
    public SqsAsyncClient configSqs() {
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(ProfileCredentialsProvider.create("default"))
                .build();
    }

    @Bean("listenerClient")
    @Profile({"dev", "qa", "pdn"})
    public SqsAsyncClient configAsyncSqs() {
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(getProviderChain())
                .build();
    }


    @Bean("fifoListener")
    public FifoListener fifoListener(@Qualifier("listenerClient") SqsAsyncClient client,
                                     @Value("${adapter.aws.sqs.purchase.queue-url}") String queueUrl,
                                     @Value("${entrypoint.aws.sqs.purchase.number-of-threads}") String numberOfThreads,
                                     @Value("${entrypoint.aws.sqs.purchase.max-number-of-messages}") String maxNumberOfMessages,
                                     @Value("${entrypoint.aws.sqs.purchase.wait-time-seconds}") String waitTimeSeconds,
                                     @Value("${entrypoint.aws.sqs.purchase.visibility-timeout-seconds}") String visibilityTimeoutSeconds,
                                     @Qualifier("sqsFifoProcessor") Function<Message, Mono<Void>> fn) {
        return FifoListener.builder()
                .client(client)
                .queueUrl(queueUrl)
                .numberOfThreads(Integer.valueOf(numberOfThreads))
                .maxNumberOfMessages(Integer.valueOf(maxNumberOfMessages))
                .waitTimeSeconds(Integer.valueOf(waitTimeSeconds))
                .visibilityTimeoutSeconds(Integer.valueOf(visibilityTimeoutSeconds))
                .processor(fn)
                .build()
                .start();
    }

    @Bean("standardListener")
    public StandardListener standardListener(@Qualifier("listenerClient") SqsAsyncClient client,
                                             @Value("${adapter.aws.sqs.release.queue-url}") String queueUrl,
                                             @Value("${entrypoint.aws.sqs.release.number-of-threads}") String numberOfThreads,
                                             @Value("${entrypoint.aws.sqs.release.max-number-of-messages}") String maxNumberOfMessages,
                                             @Value("${entrypoint.aws.sqs.release.wait-time-seconds}") String waitTimeSeconds,
                                             @Value("${entrypoint.aws.sqs.release.visibility-timeout-seconds}") String visibilityTimeoutSeconds,
                                             @Qualifier("sqsStandardProcessor") Function<Message, Mono<Void>> fn) {
        return StandardListener.builder()
                .client(client)
                .queueUrl(queueUrl)
                .numberOfThreads(Integer.valueOf(numberOfThreads))
                .maxNumberOfMessages(Integer.valueOf(maxNumberOfMessages))
                .waitTimeSeconds(Integer.valueOf(waitTimeSeconds))
                .visibilityTimeoutSeconds(Integer.valueOf(visibilityTimeoutSeconds))
                .processor(fn)
                .build()
                .start();
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

}

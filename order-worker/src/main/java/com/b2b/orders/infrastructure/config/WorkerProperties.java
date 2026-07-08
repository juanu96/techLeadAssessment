package com.b2b.orders.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "worker")
public record WorkerProperties(
        String currency,
        Services services,
        Cache cache,
        Resilience resilience,
        Kafka kafka
) {
    public record Services(URI productsUrl, URI clientsUrl, Duration responseTimeout) {
    }

    public record Cache(Duration productTtl, Duration clientTtl) {
    }

    public record Resilience(
            int maxAttempts,
            Duration waitDuration,
            float failureRateThreshold,
            int slidingWindowSize,
            Duration openStateDuration
    ) {
    }

    public record Kafka(
            boolean enabled,
            String bootstrapServers,
            String groupId,
            String ordersTopic,
            String dltTopic,
            int processingMaxAttempts,
            Duration retryDelay
    ) {
    }
}

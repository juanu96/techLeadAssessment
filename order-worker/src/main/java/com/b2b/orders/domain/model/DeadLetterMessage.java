package com.b2b.orders.domain.model;

import java.time.Instant;

public record DeadLetterMessage(
        String originalKey,
        String originalPayload,
        String originalTopic,
        int originalPartition,
        long originalOffset,
        Instant failedAt,
        String cause,
        int attempt
) {
}

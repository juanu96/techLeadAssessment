package com.b2b.orders.domain.model;

import java.time.Instant;
import java.util.List;

public record PendingOrder(
        String orderId,
        String clientId,
        String channel,
        Instant createdAt,
        List<OrderItem> items
) {
    public PendingOrder {
        requireText(orderId, "orderId");
        requireText(clientId, "clientId");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        items = List.copyOf(items);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}

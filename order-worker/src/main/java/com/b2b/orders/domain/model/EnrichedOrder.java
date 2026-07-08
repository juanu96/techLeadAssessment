package com.b2b.orders.domain.model;

import java.time.Instant;
import java.util.List;

public record EnrichedOrder(
        String orderId,
        OrderStatus status,
        Client client,
        List<CalculatedOrderLine> items,
        OrderSummary summary,
        Instant processedAt
) {
    public EnrichedOrder {
        items = List.copyOf(items);
    }
}

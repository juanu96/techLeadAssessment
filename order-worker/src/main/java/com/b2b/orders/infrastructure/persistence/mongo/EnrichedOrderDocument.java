package com.b2b.orders.infrastructure.persistence.mongo;

import com.b2b.orders.domain.model.CalculatedOrderLine;
import com.b2b.orders.domain.model.Client;
import com.b2b.orders.domain.model.EnrichedOrder;
import com.b2b.orders.domain.model.OrderStatus;
import com.b2b.orders.domain.model.OrderSummary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "enriched-orders")
public record EnrichedOrderDocument(
        @Id String orderId,
        OrderStatus status,
        Client client,
        List<CalculatedOrderLine> items,
        OrderSummary summary,
        Instant processedAt
) {
    static EnrichedOrderDocument fromDomain(EnrichedOrder order) {
        return new EnrichedOrderDocument(
                order.orderId(),
                order.status(),
                order.client(),
                order.items(),
                order.summary(),
                order.processedAt()
        );
    }

    EnrichedOrder toDomain() {
        return new EnrichedOrder(orderId, status, client, items, summary, processedAt);
    }
}

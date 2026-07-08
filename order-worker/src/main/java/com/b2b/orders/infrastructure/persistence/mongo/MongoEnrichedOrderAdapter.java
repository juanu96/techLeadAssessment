package com.b2b.orders.infrastructure.persistence.mongo;

import com.b2b.orders.application.port.out.EnrichedOrderRepositoryPort;
import com.b2b.orders.domain.model.EnrichedOrder;
import com.b2b.orders.domain.model.OrderStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MongoEnrichedOrderAdapter implements EnrichedOrderRepositoryPort {

    private final MongoEnrichedOrderRepository repository;

    public MongoEnrichedOrderAdapter(MongoEnrichedOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Boolean> existsProcessedByOrderId(String orderId) {
        return repository.existsByOrderIdAndStatus(orderId, OrderStatus.PROCESSED);
    }

    @Override
    public Mono<EnrichedOrder> save(EnrichedOrder order) {
        return repository.save(EnrichedOrderDocument.fromDomain(order))
                .map(EnrichedOrderDocument::toDomain);
    }
}

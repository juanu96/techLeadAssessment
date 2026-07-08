package com.b2b.orders.infrastructure.persistence.mongo;

import com.b2b.orders.domain.model.OrderStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

interface MongoEnrichedOrderRepository
        extends ReactiveMongoRepository<EnrichedOrderDocument, String> {

    Mono<Boolean> existsByOrderIdAndStatus(String orderId, OrderStatus status);
}

package com.b2b.orders.application.port.out;

import com.b2b.orders.domain.model.EnrichedOrder;
import reactor.core.publisher.Mono;

public interface EnrichedOrderRepositoryPort {

    Mono<Boolean> existsProcessedByOrderId(String orderId);

    Mono<EnrichedOrder> save(EnrichedOrder order);
}

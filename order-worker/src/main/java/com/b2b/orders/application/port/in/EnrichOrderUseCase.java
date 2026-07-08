package com.b2b.orders.application.port.in;

import com.b2b.orders.domain.model.EnrichedOrder;
import com.b2b.orders.domain.model.PendingOrder;
import reactor.core.publisher.Mono;

public interface EnrichOrderUseCase {

    Mono<EnrichedOrder> enrich(PendingOrder order);
}

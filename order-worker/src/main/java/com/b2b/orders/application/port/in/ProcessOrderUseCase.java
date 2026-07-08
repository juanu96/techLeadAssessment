package com.b2b.orders.application.port.in;

import com.b2b.orders.domain.model.OrderProcessingResult;
import com.b2b.orders.domain.model.PendingOrder;
import reactor.core.publisher.Mono;

public interface ProcessOrderUseCase {

    Mono<OrderProcessingResult> process(PendingOrder order);
}

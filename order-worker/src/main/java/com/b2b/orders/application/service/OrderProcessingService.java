package com.b2b.orders.application.service;

import com.b2b.orders.application.port.in.EnrichOrderUseCase;
import com.b2b.orders.application.port.in.ProcessOrderUseCase;
import com.b2b.orders.application.port.out.EnrichedOrderRepositoryPort;
import com.b2b.orders.domain.model.OrderProcessingResult;
import com.b2b.orders.domain.model.PendingOrder;
import reactor.core.publisher.Mono;

public class OrderProcessingService implements ProcessOrderUseCase {

    private final EnrichOrderUseCase enrichmentService;
    private final EnrichedOrderRepositoryPort repository;

    public OrderProcessingService(
            EnrichOrderUseCase enrichmentService,
            EnrichedOrderRepositoryPort repository
    ) {
        this.enrichmentService = enrichmentService;
        this.repository = repository;
    }

    @Override
    public Mono<OrderProcessingResult> process(PendingOrder order) {
        return repository.existsProcessedByOrderId(order.orderId())
                .flatMap(alreadyProcessed -> alreadyProcessed
                        ? Mono.just(OrderProcessingResult.ALREADY_PROCESSED)
                        : enrichAndStore(order));
    }

    private Mono<OrderProcessingResult> enrichAndStore(PendingOrder order) {
        return enrichmentService.enrich(order)
                .flatMap(repository::save)
                .thenReturn(OrderProcessingResult.PROCESSED);
    }
}

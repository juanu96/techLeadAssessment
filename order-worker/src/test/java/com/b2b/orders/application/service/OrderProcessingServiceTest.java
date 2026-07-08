package com.b2b.orders.application.service;

import com.b2b.orders.application.port.in.EnrichOrderUseCase;
import com.b2b.orders.application.port.out.EnrichedOrderRepositoryPort;
import com.b2b.orders.domain.model.Client;
import com.b2b.orders.domain.model.ClientSegment;
import com.b2b.orders.domain.model.EnrichedOrder;
import com.b2b.orders.domain.model.OrderItem;
import com.b2b.orders.domain.model.OrderProcessingResult;
import com.b2b.orders.domain.model.OrderStatus;
import com.b2b.orders.domain.model.OrderSummary;
import com.b2b.orders.domain.model.PendingOrder;
import com.b2b.orders.domain.model.TaxRegime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private EnrichOrderUseCase enrichmentService;
    @Mock
    private EnrichedOrderRepositoryPort repository;

    private OrderProcessingService service;

    @BeforeEach
    void setUp() {
        service = new OrderProcessingService(enrichmentService, repository);
    }

    @Test
    void enrichesAndStoresNewOrders() {
        PendingOrder order = pendingOrder();
        EnrichedOrder enrichedOrder = enrichedOrder();
        when(repository.existsProcessedByOrderId(order.orderId())).thenReturn(Mono.just(false));
        when(enrichmentService.enrich(order)).thenReturn(Mono.just(enrichedOrder));
        when(repository.save(enrichedOrder)).thenReturn(Mono.just(enrichedOrder));

        StepVerifier.create(service.process(order))
                .expectNext(OrderProcessingResult.PROCESSED)
                .verifyComplete();

        verify(repository).save(enrichedOrder);
    }

    @Test
    void skipsOrdersThatWereAlreadyProcessed() {
        PendingOrder order = pendingOrder();
        when(repository.existsProcessedByOrderId(order.orderId())).thenReturn(Mono.just(true));

        StepVerifier.create(service.process(order))
                .expectNext(OrderProcessingResult.ALREADY_PROCESSED)
                .verifyComplete();

        verifyNoInteractions(enrichmentService);
        verify(repository, never()).save(enrichedOrder());
    }

    private PendingOrder pendingOrder() {
        return new PendingOrder(
                "ORD-001",
                "CLI-001",
                "B2B",
                Instant.parse("2024-09-12T10:45:00Z"),
                List.of(new OrderItem("PRD-001", BigDecimal.ONE, new BigDecimal("100.00")))
        );
    }

    private EnrichedOrder enrichedOrder() {
        return new EnrichedOrder(
                "ORD-001",
                OrderStatus.PROCESSED,
                new Client(
                        "CLI-001",
                        "Cliente",
                        ClientSegment.MAYORISTA,
                        TaxRegime.RESPONSABLE_IVA,
                        "Centro",
                        "B2B"
                ),
                List.of(),
                new OrderSummary(
                        new BigDecimal("100.00"),
                        new BigDecimal("19.00"),
                        new BigDecimal("119.00"),
                        "COP"
                ),
                Instant.parse("2024-09-12T10:45:03Z")
        );
    }
}

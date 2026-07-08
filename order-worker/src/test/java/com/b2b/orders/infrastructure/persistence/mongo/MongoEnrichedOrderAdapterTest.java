package com.b2b.orders.infrastructure.persistence.mongo;

import com.b2b.orders.domain.model.Client;
import com.b2b.orders.domain.model.ClientSegment;
import com.b2b.orders.domain.model.EnrichedOrder;
import com.b2b.orders.domain.model.OrderStatus;
import com.b2b.orders.domain.model.OrderSummary;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoEnrichedOrderAdapterTest {

    @Mock
    private MongoEnrichedOrderRepository repository;

    private MongoEnrichedOrderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MongoEnrichedOrderAdapter(repository);
    }

    @Test
    void checksProcessedOrdersByOrderIdAndStatus() {
        when(repository.existsByOrderIdAndStatus("ORD-001", OrderStatus.PROCESSED))
                .thenReturn(Mono.just(true));

        StepVerifier.create(adapter.existsProcessedByOrderId("ORD-001"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void mapsAndStoresEnrichedOrders() {
        EnrichedOrder order = enrichedOrder();
        EnrichedOrderDocument document = EnrichedOrderDocument.fromDomain(order);
        when(repository.save(document)).thenReturn(Mono.just(document));

        StepVerifier.create(adapter.save(order))
                .expectNext(order)
                .verifyComplete();

        verify(repository).save(document);
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

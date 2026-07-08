package com.b2b.orders.infrastructure.messaging.kafka;

import com.b2b.orders.application.port.in.ProcessOrderUseCase;
import com.b2b.orders.application.port.out.DeadLetterPublisherPort;
import com.b2b.orders.domain.model.DeadLetterMessage;
import com.b2b.orders.domain.model.OrderItem;
import com.b2b.orders.domain.model.OrderProcessingResult;
import com.b2b.orders.domain.model.PendingOrder;
import com.b2b.orders.infrastructure.config.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaOrderRecordHandlerTest {

    private static final Instant FAILED_AT = Instant.parse("2024-09-12T10:45:10Z");

    @Mock
    private ProcessOrderUseCase processOrder;
    @Mock
    private DeadLetterPublisherPort deadLetterPublisher;
    @Mock
    private ReceiverRecord<String, String> record;
    @Mock
    private ReceiverOffset receiverOffset;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        when(record.receiverOffset()).thenReturn(receiverOffset);
        when(receiverOffset.commit()).thenReturn(Mono.empty());
    }

    @Test
    void processesValidOrdersAndCommitsTheirOffset() throws Exception {
        PendingOrder order = pendingOrder();
        when(record.value()).thenReturn(objectMapper.writeValueAsString(order));
        when(processOrder.process(order)).thenReturn(Mono.just(OrderProcessingResult.PROCESSED));

        StepVerifier.create(handler(3).handle(record)).verifyComplete();

        verify(processOrder).process(order);
        verify(deadLetterPublisher, never()).publish(any());
        verify(receiverOffset).commit();
    }

    @Test
    void retriesProcessingFailuresAndPublishesTheirMetadataToDlt() throws Exception {
        PendingOrder order = pendingOrder();
        when(record.value()).thenReturn(objectMapper.writeValueAsString(order));
        when(record.key()).thenReturn(order.orderId());
        when(record.topic()).thenReturn("orders-topic");
        when(record.partition()).thenReturn(2);
        when(record.offset()).thenReturn(17L);
        when(processOrder.process(order))
                .thenReturn(Mono.error(new IllegalStateException("products api unavailable")));
        when(deadLetterPublisher.publish(any())).thenReturn(Mono.empty());

        StepVerifier.create(handler(3).handle(record)).verifyComplete();

        ArgumentCaptor<DeadLetterMessage> message = ArgumentCaptor.forClass(DeadLetterMessage.class);
        verify(processOrder, times(3)).process(order);
        verify(deadLetterPublisher).publish(message.capture());
        assertThat(message.getValue().originalPayload()).isEqualTo(record.value());
        assertThat(message.getValue().originalTopic()).isEqualTo("orders-topic");
        assertThat(message.getValue().originalPartition()).isEqualTo(2);
        assertThat(message.getValue().originalOffset()).isEqualTo(17L);
        assertThat(message.getValue().failedAt()).isEqualTo(FAILED_AT);
        assertThat(message.getValue().cause()).isEqualTo("products api unavailable");
        assertThat(message.getValue().attempt()).isEqualTo(3);
        verify(receiverOffset).commit();
    }

    @Test
    void sendsInvalidMessagesToDltWithoutRetrying() {
        when(record.value()).thenReturn("{invalid-json");
        when(record.topic()).thenReturn("orders-topic");
        when(deadLetterPublisher.publish(any())).thenReturn(Mono.empty());

        StepVerifier.create(handler(3).handle(record)).verifyComplete();

        ArgumentCaptor<DeadLetterMessage> message = ArgumentCaptor.forClass(DeadLetterMessage.class);
        verify(processOrder, never()).process(any());
        verify(deadLetterPublisher).publish(message.capture());
        assertThat(message.getValue().attempt()).isEqualTo(1);
        assertThat(message.getValue().cause()).isEqualTo("Order message is invalid");
        verify(receiverOffset).commit();
    }

    private KafkaOrderRecordHandler handler(int maxAttempts) {
        WorkerProperties properties = new WorkerProperties(
                null,
                null,
                null,
                null,
                new WorkerProperties.Kafka(
                        true,
                        "localhost:9092",
                        "order-worker-test",
                        "orders-topic",
                        "orders-dlt",
                        maxAttempts,
                        Duration.ofMillis(1)
                )
        );
        return new KafkaOrderRecordHandler(
                processOrder,
                deadLetterPublisher,
                objectMapper,
                properties,
                Clock.fixed(FAILED_AT, ZoneOffset.UTC)
        );
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
}

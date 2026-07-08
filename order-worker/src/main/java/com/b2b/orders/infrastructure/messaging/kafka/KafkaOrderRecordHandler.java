package com.b2b.orders.infrastructure.messaging.kafka;

import com.b2b.orders.application.port.in.ProcessOrderUseCase;
import com.b2b.orders.application.port.out.DeadLetterPublisherPort;
import com.b2b.orders.domain.model.DeadLetterMessage;
import com.b2b.orders.domain.model.PendingOrder;
import com.b2b.orders.infrastructure.config.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(
        prefix = "worker.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaOrderRecordHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaOrderRecordHandler.class);

    private final ProcessOrderUseCase processOrder;
    private final DeadLetterPublisherPort deadLetterPublisher;
    private final ObjectMapper objectMapper;
    private final WorkerProperties.Kafka properties;
    private final Clock clock;

    public KafkaOrderRecordHandler(
            ProcessOrderUseCase processOrder,
            DeadLetterPublisherPort deadLetterPublisher,
            ObjectMapper objectMapper,
            WorkerProperties properties,
            Clock clock
    ) {
        this.processOrder = processOrder;
        this.deadLetterPublisher = deadLetterPublisher;
        this.objectMapper = objectMapper;
        this.properties = properties.kafka();
        this.clock = clock;
    }

    public Mono<Void> handle(ReceiverRecord<String, String> record) {
        AtomicInteger attempts = new AtomicInteger();
        Mono<Void> processing = Mono.defer(() -> process(record.value(), attempts.incrementAndGet()));

        if (properties.processingMaxAttempts() > 1) {
            processing = processing.retryWhen(Retry
                    .fixedDelay(properties.processingMaxAttempts() - 1, properties.retryDelay())
                    .filter(error -> !(error instanceof InvalidOrderMessageException)));
        }

        return processing
                .onErrorResume(error -> publishToDlt(record, error, attempts.get()))
                .then(record.receiverOffset().commit());
    }

    private Mono<Void> process(String payload, int attempt) {
        return deserialize(payload)
                .doOnNext(order -> LOGGER.atInfo()
                        .addKeyValue("orderId", order.orderId())
                        .addKeyValue("attempt", attempt)
                        .log("Processing order"))
                .flatMap(order -> processOrder.process(order)
                        .doOnNext(result -> LOGGER.atInfo()
                                .addKeyValue("orderId", order.orderId())
                                .addKeyValue("result", result)
                                .log("Order processing finished")))
                .then();
    }

    private Mono<PendingOrder> deserialize(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, PendingOrder.class))
                .onErrorMap(error -> new InvalidOrderMessageException(
                        "Order message is invalid",
                        error
                ));
    }

    private Mono<Void> publishToDlt(
            ReceiverRecord<String, String> record,
            Throwable error,
            int attempt
    ) {
        Throwable cause = rootCause(error);
        DeadLetterMessage message = new DeadLetterMessage(
                record.key(),
                record.value(),
                record.topic(),
                record.partition(),
                record.offset(),
                Instant.now(clock),
                errorMessage(cause),
                attempt
        );

        LOGGER.atError()
                .addKeyValue("messageKey", record.key())
                .addKeyValue("attempt", attempt)
                .setCause(cause)
                .log("Order processing failed; publishing to DLT");
        return deadLetterPublisher.publish(message);
    }

    private Throwable rootCause(Throwable error) {
        if (Exceptions.isRetryExhausted(error) && error.getCause() != null) {
            return error.getCause();
        }
        return Exceptions.unwrap(error);
    }

    private String errorMessage(Throwable error) {
        return error.getMessage() == null
                ? error.getClass().getSimpleName()
                : error.getMessage();
    }
}

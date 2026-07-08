package com.b2b.orders.infrastructure.messaging.kafka;

import com.b2b.orders.application.port.out.DeadLetterPublisherPort;
import com.b2b.orders.domain.model.DeadLetterMessage;
import com.b2b.orders.infrastructure.config.WorkerProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Component
@ConditionalOnProperty(
        prefix = "worker.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaDeadLetterPublisher implements DeadLetterPublisherPort {

    private final KafkaSender<String, String> sender;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaDeadLetterPublisher(
            KafkaSender<String, String> sender,
            ObjectMapper objectMapper,
            WorkerProperties properties
    ) {
        this.sender = sender;
        this.objectMapper = objectMapper;
        this.topic = properties.kafka().dltTopic();
    }

    @Override
    public Mono<Void> publish(DeadLetterMessage message) {
        return serialize(message)
                .flatMap(payload -> sender.send(Mono.just(SenderRecord.create(
                                new ProducerRecord<>(topic, message.originalKey(), payload),
                                message
                        )))
                        .next())
                .flatMap(result -> result.exception() == null
                        ? Mono.<Void>empty()
                        : Mono.error(result.exception()));
    }

    private Mono<String> serialize(DeadLetterMessage message) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(message))
                .onErrorMap(JsonProcessingException.class,
                        error -> new IllegalStateException("Could not serialize DLT message", error));
    }
}

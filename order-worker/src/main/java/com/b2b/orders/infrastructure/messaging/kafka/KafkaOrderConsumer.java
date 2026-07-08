package com.b2b.orders.infrastructure.messaging.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.kafka.receiver.KafkaReceiver;

@Component
@ConditionalOnProperty(
        prefix = "worker.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaOrderConsumer implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaOrderConsumer.class);

    private final KafkaReceiver<String, String> receiver;
    private final KafkaOrderRecordHandler handler;
    private Disposable subscription;

    public KafkaOrderConsumer(
            KafkaReceiver<String, String> receiver,
            KafkaOrderRecordHandler handler
    ) {
        this.receiver = receiver;
        this.handler = handler;
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        subscription = receiver.receive()
                .concatMap(handler::handle)
                .subscribe(
                        ignored -> { },
                        error -> LOGGER.error("Kafka order consumer stopped", error)
                );
    }

    @Override
    public void stop() {
        if (subscription != null) {
            subscription.dispose();
            subscription = null;
        }
    }

    @Override
    public boolean isRunning() {
        return subscription != null && !subscription.isDisposed();
    }
}

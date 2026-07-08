package com.b2b.orders.application.port.out;

import com.b2b.orders.domain.model.DeadLetterMessage;
import reactor.core.publisher.Mono;

public interface DeadLetterPublisherPort {

    Mono<Void> publish(DeadLetterMessage message);
}

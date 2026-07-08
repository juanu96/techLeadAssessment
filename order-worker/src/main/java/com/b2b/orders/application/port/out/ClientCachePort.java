package com.b2b.orders.application.port.out;

import com.b2b.orders.domain.model.Client;
import reactor.core.publisher.Mono;

public interface ClientCachePort {

    Mono<Client> findById(String clientId);

    Mono<Void> save(Client client);
}

package com.b2b.orders.application.port.out;

import com.b2b.orders.domain.model.Client;
import reactor.core.publisher.Mono;

public interface ClientCatalogPort {

    Mono<Client> findById(String clientId);
}

package com.b2b.orders.infrastructure.http;

import com.b2b.orders.application.port.out.ClientCatalogPort;
import com.b2b.orders.domain.model.Client;
import com.b2b.orders.infrastructure.http.dto.ClientResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class ClientsWebClientAdapter implements ClientCatalogPort {

    private final WebClient webClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public ClientsWebClientAdapter(
            WebClient webClient,
            Retry retry,
            CircuitBreaker circuitBreaker
    ) {
        this.webClient = webClient;
        this.retry = retry;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Mono<Client> findById(String clientId) {
        return webClient.get()
                .uri("/clients/{clientId}", clientId)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> response.releaseBody().thenReturn(
                                new ExternalResourceNotFoundException(
                                        "client not found: " + clientId
                                )
                        )
                )
                .bodyToMono(ClientResponse.class)
                .switchIfEmpty(Mono.error(new ExternalResourceNotFoundException(
                        "empty client response: " + clientId
                )))
                .map(ClientResponse::toDomain)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));
    }
}

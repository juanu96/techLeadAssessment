package com.b2b.orders.infrastructure.http;

import com.b2b.orders.application.port.out.ProductCatalogPort;
import com.b2b.orders.domain.model.Product;
import com.b2b.orders.infrastructure.http.dto.ProductResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class ProductsWebClientAdapter implements ProductCatalogPort {

    private final WebClient webClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public ProductsWebClientAdapter(
            WebClient webClient,
            Retry retry,
            CircuitBreaker circuitBreaker
    ) {
        this.webClient = webClient;
        this.retry = retry;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Mono<Product> findById(String productId) {
        return webClient.get()
                .uri("/products/{productId}", productId)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> response.releaseBody().thenReturn(
                                new ExternalResourceNotFoundException(
                                        "product not found: " + productId
                                )
                        )
                )
                .bodyToMono(ProductResponse.class)
                .switchIfEmpty(Mono.error(new ExternalResourceNotFoundException(
                        "empty product response: " + productId
                )))
                .map(ProductResponse::toDomain)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));
    }
}

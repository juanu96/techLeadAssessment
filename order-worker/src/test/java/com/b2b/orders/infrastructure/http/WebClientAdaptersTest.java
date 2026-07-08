package com.b2b.orders.infrastructure.http;

import com.b2b.orders.domain.model.ClientSegment;
import com.b2b.orders.domain.model.TaxCategory;
import com.b2b.orders.domain.model.TaxRegime;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientAdaptersTest {

    @Test
    void retriesTransientProductFailures() {
        AtomicInteger attempts = new AtomicInteger();
        ExchangeFunction exchange = request -> {
            if (attempts.incrementAndGet() == 1) {
                return response(HttpStatus.INTERNAL_SERVER_ERROR, "");
            }
            return response(HttpStatus.OK, """
                    {
                      "productId": "PRD-001",
                      "name": "Gaseosa 600ml",
                      "sku": "GAS-600-PET",
                      "category": "Bebidas",
                      "taxCategory": "GRAVADO",
                      "unitOfMeasure": "UNIDAD"
                    }
                    """);
        };
        var adapter = new ProductsWebClientAdapter(
                webClient(exchange),
                retry(2),
                CircuitBreaker.ofDefaults("products-test")
        );

        StepVerifier.create(adapter.findById("PRD-001"))
                .assertNext(product -> {
                    assertThat(product.productId()).isEqualTo("PRD-001");
                    assertThat(product.taxCategory()).isEqualTo(TaxCategory.GRAVADO);
                })
                .verifyComplete();

        assertThat(attempts).hasValue(2);
    }

    @Test
    void doesNotRetryMissingProducts() {
        AtomicInteger attempts = new AtomicInteger();
        ExchangeFunction exchange = request -> {
            attempts.incrementAndGet();
            return response(HttpStatus.NOT_FOUND, "{\"error\":\"product not found\"}");
        };
        var adapter = new ProductsWebClientAdapter(
                webClient(exchange),
                retry(3),
                CircuitBreaker.ofDefaults("products-not-found-test")
        );

        StepVerifier.create(adapter.findById("PRD-404"))
                .expectErrorMatches(error -> error instanceof ExternalResourceNotFoundException
                        && error.getMessage().equals("product not found: PRD-404"))
                .verify();

        assertThat(attempts).hasValue(1);
    }

    @Test
    void mapsClientResponses() {
        ExchangeFunction exchange = request -> response(HttpStatus.OK, """
                {
                  "clientId": "CLI-99821",
                  "name": "Distribuidora Andina S.A.S",
                  "segment": "MAYORISTA",
                  "taxRegime": "RESPONSABLE_IVA",
                  "region": "Valle del Cauca",
                  "channel": "B2B"
                }
                """);
        var adapter = new ClientsWebClientAdapter(
                webClient(exchange),
                retry(2),
                CircuitBreaker.ofDefaults("clients-test")
        );

        StepVerifier.create(adapter.findById("CLI-99821"))
                .assertNext(client -> {
                    assertThat(client.segment()).isEqualTo(ClientSegment.MAYORISTA);
                    assertThat(client.taxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
                })
                .verifyComplete();
    }

    private WebClient webClient(ExchangeFunction exchange) {
        return WebClient.builder()
                .baseUrl("http://catalog.test")
                .exchangeFunction(exchange)
                .build();
    }

    private Retry retry(int maxAttempts) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ZERO)
                .ignoreExceptions(ExternalResourceNotFoundException.class)
                .build();
        return Retry.of("retry-" + maxAttempts, config);
    }

    private Mono<ClientResponse> response(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(body)
                .build());
    }
}

package com.b2b.orders.infrastructure.config;

import com.b2b.orders.application.port.in.CalculateOrderTaxesUseCase;
import com.b2b.orders.application.port.in.EnrichOrderUseCase;
import com.b2b.orders.application.port.out.ClientCachePort;
import com.b2b.orders.application.port.out.ClientCatalogPort;
import com.b2b.orders.application.port.out.ProductCachePort;
import com.b2b.orders.application.port.out.ProductCatalogPort;
import com.b2b.orders.application.service.OrderEnrichmentService;
import com.b2b.orders.application.service.OrderTaxService;
import com.b2b.orders.domain.service.TaxCalculator;
import com.b2b.orders.infrastructure.cache.RedisClientCacheAdapter;
import com.b2b.orders.infrastructure.cache.RedisProductCacheAdapter;
import com.b2b.orders.infrastructure.http.ClientsWebClientAdapter;
import com.b2b.orders.infrastructure.http.ExternalResourceNotFoundException;
import com.b2b.orders.infrastructure.http.ProductsWebClientAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(WorkerProperties.class)
public class WorkerConfiguration {

    @Bean
    TaxCalculator taxCalculator() {
        return new TaxCalculator();
    }

    @Bean
    CalculateOrderTaxesUseCase calculateOrderTaxesUseCase(TaxCalculator taxCalculator) {
        return new OrderTaxService(taxCalculator);
    }

    @Bean
    ProductCatalogPort productCatalogPort(
            WebClient.Builder builder,
            WorkerProperties properties
    ) {
        return new ProductsWebClientAdapter(
                webClient(
                        builder,
                        properties.services().productsUrl(),
                        properties.services().responseTimeout()
                ),
                retry("products-api", properties.resilience()),
                circuitBreaker("products-api", properties.resilience())
        );
    }

    @Bean
    ClientCatalogPort clientCatalogPort(
            WebClient.Builder builder,
            WorkerProperties properties
    ) {
        return new ClientsWebClientAdapter(
                webClient(
                        builder,
                        properties.services().clientsUrl(),
                        properties.services().responseTimeout()
                ),
                retry("clients-api", properties.resilience()),
                circuitBreaker("clients-api", properties.resilience())
        );
    }

    @Bean
    ProductCachePort productCachePort(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            WorkerProperties properties
    ) {
        return new RedisProductCacheAdapter(
                redisTemplate,
                objectMapper,
                properties.cache().productTtl()
        );
    }

    @Bean
    ClientCachePort clientCachePort(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            WorkerProperties properties
    ) {
        return new RedisClientCacheAdapter(
                redisTemplate,
                objectMapper,
                properties.cache().clientTtl()
        );
    }

    @Bean
    EnrichOrderUseCase enrichOrderUseCase(
            ProductCatalogPort productCatalog,
            ClientCatalogPort clientCatalog,
            ProductCachePort productCache,
            ClientCachePort clientCache,
            CalculateOrderTaxesUseCase taxService,
            WorkerProperties properties,
            Clock clock
    ) {
        return new OrderEnrichmentService(
                productCatalog,
                clientCatalog,
                productCache,
                clientCache,
                taxService,
                properties.currency(),
                clock
        );
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    private WebClient webClient(WebClient.Builder builder, URI baseUrl, Duration timeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(timeout.toMillis()))
                .responseTimeout(timeout);
        return builder.clone()
                .baseUrl(baseUrl.toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private Retry retry(String name, WorkerProperties.Resilience properties) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.maxAttempts())
                .waitDuration(properties.waitDuration())
                .ignoreExceptions(ExternalResourceNotFoundException.class)
                .build();
        return Retry.of(name, config);
    }

    private CircuitBreaker circuitBreaker(
            String name,
            WorkerProperties.Resilience properties
    ) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.failureRateThreshold())
                .slidingWindowSize(properties.slidingWindowSize())
                .waitDurationInOpenState(properties.openStateDuration())
                .ignoreExceptions(ExternalResourceNotFoundException.class)
                .build();
        return CircuitBreaker.of(name, config);
    }
}

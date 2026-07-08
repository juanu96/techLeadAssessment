package com.b2b.orders;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "worker.currency=COP",
                "worker.services.products-url=http://localhost:8081",
                "worker.services.clients-url=http://localhost:8082",
                "worker.services.response-timeout=3s",
                "worker.cache.product-ttl=300s",
                "worker.cache.client-ttl=300s",
                "worker.resilience.max-attempts=3",
                "worker.resilience.wait-duration=200ms",
                "worker.resilience.failure-rate-threshold=50",
                "worker.resilience.sliding-window-size=10",
                "worker.resilience.open-state-duration=30s",
                "worker.kafka.enabled=false"
        }
)
class OrderWorkerApplicationTest {

    @Test
    void loadsApplicationContext() {
    }
}

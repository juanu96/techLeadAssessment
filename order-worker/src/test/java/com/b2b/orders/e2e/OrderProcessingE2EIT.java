package com.b2b.orders.e2e;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OrderProcessingE2EIT {

    private static final String ORDERS_TOPIC = "orders-topic-e2e";
    private static final String DLT_TOPIC = "orders-dlt-e2e";
    private static final String ORDER_ID = "ORD-E2E-001";

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.1")
    );

    @Container
    private static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:7.0.14");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4.1-alpine")
    ).withExposedPorts(6379);

    private static final MockWebServer PRODUCTS_API = startServer();
    private static final MockWebServer CLIENTS_API = startServer();

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void configureApplication(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("worker.services.products-url", () -> PRODUCTS_API.url("/").toString());
        registry.add("worker.services.clients-url", () -> CLIENTS_API.url("/").toString());
        registry.add("worker.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("worker.kafka.group-id", () -> "order-worker-e2e");
        registry.add("worker.kafka.orders-topic", () -> ORDERS_TOPIC);
        registry.add("worker.kafka.dlt-topic", () -> DLT_TOPIC);
        registry.add("worker.kafka.processing-max-attempts", () -> 2);
        registry.add("worker.kafka.retry-delay", () -> "10ms");
    }

    @BeforeAll
    static void createTopics() throws Exception {
        Map<String, Object> properties = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()
        );
        try (AdminClient adminClient = AdminClient.create(properties)) {
            adminClient.createTopics(List.of(
                    new NewTopic(ORDERS_TOPIC, 1, (short) 1),
                    new NewTopic(DLT_TOPIC, 1, (short) 1)
            )).all().get(20, TimeUnit.SECONDS);
        }
    }

    @AfterAll
    static void stopMockApis() throws IOException {
        PRODUCTS_API.shutdown();
        CLIENTS_API.shutdown();
    }

    @Test
    void consumesAnOrderAndStoresTheEnrichedResultInMongo() throws Exception {
        PRODUCTS_API.enqueue(jsonResponse("""
                {
                  "productId": "PRD-001",
                  "name": "Gaseosa 600ml",
                  "sku": "GAS-600-PET",
                  "category": "Bebidas",
                  "taxCategory": "GRAVADO",
                  "unitOfMeasure": "UNIDAD"
                }
                """));
        CLIENTS_API.enqueue(jsonResponse("""
                {
                  "clientId": "CLI-99821",
                  "name": "Distribuidora Andina S.A.S",
                  "segment": "MAYORISTA",
                  "taxRegime": "RESPONSABLE_IVA",
                  "region": "Valle del Cauca",
                  "channel": "B2B"
                }
                """));

        publishOrder("""
                {
                  "orderId": "ORD-E2E-001",
                  "clientId": "CLI-99821",
                  "channel": "B2B",
                  "createdAt": "2024-09-12T10:45:00Z",
                  "items": [
                    {
                      "productId": "PRD-001",
                      "quantity": 1,
                      "unitPrice": 100.00
                    }
                  ]
                }
                """);

        await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> StepVerifier
                        .create(mongoTemplate.findById(
                                ORDER_ID,
                                Document.class,
                                "enriched-orders"
                        ))
                        .assertNext(document -> {
                            assertThat(document.getString("status")).isEqualTo("PROCESSED");
                            assertThat(document.get("client", Document.class).getString("clientId"))
                                    .isEqualTo("CLI-99821");
                            assertThat(document.get("summary", Document.class).get("grandTotal").toString())
                                    .isEqualTo("119.00");
                        })
                        .verifyComplete());
    }

    private void publishOrder(String payload) throws Exception {
        Map<String, Object> properties = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"
        );
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            producer.send(new ProducerRecord<>(ORDERS_TOPIC, ORDER_ID, payload))
                    .get(10, TimeUnit.SECONDS);
        }
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static MockWebServer startServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException("Could not start mock catalog API", error);
        }
    }
}

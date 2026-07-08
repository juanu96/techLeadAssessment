package com.b2b.orders.infrastructure.messaging.kafka;

import com.b2b.orders.infrastructure.config.WorkerProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Configuration
@ConditionalOnProperty(
        prefix = "worker.kafka",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaConfiguration {

    @Bean
    KafkaReceiver<String, String> kafkaReceiver(WorkerProperties properties) {
        Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.kafka().bootstrapServers()
        );
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, properties.kafka().groupId());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ReceiverOptions<String, String> options = ReceiverOptions
                .<String, String>create(consumerProperties)
                .commitInterval(Duration.ZERO)
                .commitBatchSize(0)
                .subscription(Set.of(properties.kafka().ordersTopic()));

        return KafkaReceiver.create(options);
    }

    @Bean
    KafkaSender<String, String> kafkaSender(WorkerProperties properties) {
        Map<String, Object> producerProperties = new HashMap<>();
        producerProperties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.kafka().bootstrapServers()
        );
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");

        return KafkaSender.create(SenderOptions.create(producerProperties));
    }
}

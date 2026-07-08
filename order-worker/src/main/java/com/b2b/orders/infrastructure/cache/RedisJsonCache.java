package com.b2b.orders.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

abstract class RedisJsonCache<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Class<T> valueType;
    private final String keyPrefix;
    private final Duration ttl;

    protected RedisJsonCache(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Class<T> valueType,
            String keyPrefix,
            Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.valueType = valueType;
        this.keyPrefix = keyPrefix;
        this.ttl = ttl;
    }

    protected Mono<T> find(String id) {
        String key = keyPrefix + id;
        return redisTemplate.opsForValue()
                .get(key)
                .flatMap(this::deserialize)
                .onErrorResume(error -> {
                    logger.warn("cache read failed for key={}", key, error);
                    return Mono.empty();
                });
    }

    protected Mono<Void> save(String id, T value) {
        String key = keyPrefix + id;
        return serialize(value)
                .flatMap(json -> redisTemplate.opsForValue().set(key, json, ttl))
                .then()
                .onErrorResume(error -> {
                    logger.warn("cache write failed for key={}", key, error);
                    return Mono.empty();
                });
    }

    private Mono<T> deserialize(String json) {
        return Mono.fromCallable(() -> objectMapper.readValue(json, valueType));
    }

    private Mono<String> serialize(T value) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(value));
    }
}

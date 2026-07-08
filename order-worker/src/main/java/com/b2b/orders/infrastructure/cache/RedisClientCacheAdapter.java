package com.b2b.orders.infrastructure.cache;

import com.b2b.orders.application.port.out.ClientCachePort;
import com.b2b.orders.domain.model.Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class RedisClientCacheAdapter extends RedisJsonCache<Client> implements ClientCachePort {

    public RedisClientCacheAdapter(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Duration ttl
    ) {
        super(redisTemplate, objectMapper, Client.class, "clients:", ttl);
    }

    @Override
    public Mono<Client> findById(String clientId) {
        return find(clientId);
    }

    @Override
    public Mono<Void> save(Client client) {
        return save(client.clientId(), client);
    }
}

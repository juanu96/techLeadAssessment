package com.b2b.orders.infrastructure.cache;

import com.b2b.orders.application.port.out.ProductCachePort;
import com.b2b.orders.domain.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class RedisProductCacheAdapter extends RedisJsonCache<Product> implements ProductCachePort {

    public RedisProductCacheAdapter(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Duration ttl
    ) {
        super(redisTemplate, objectMapper, Product.class, "products:", ttl);
    }

    @Override
    public Mono<Product> findById(String productId) {
        return find(productId);
    }

    @Override
    public Mono<Void> save(Product product) {
        return save(product.productId(), product);
    }
}

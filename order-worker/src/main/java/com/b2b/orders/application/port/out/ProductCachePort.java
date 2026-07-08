package com.b2b.orders.application.port.out;

import com.b2b.orders.domain.model.Product;
import reactor.core.publisher.Mono;

public interface ProductCachePort {

    Mono<Product> findById(String productId);

    Mono<Void> save(Product product);
}

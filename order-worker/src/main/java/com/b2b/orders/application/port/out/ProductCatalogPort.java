package com.b2b.orders.application.port.out;

import com.b2b.orders.domain.model.Product;
import reactor.core.publisher.Mono;

public interface ProductCatalogPort {

    Mono<Product> findById(String productId);
}

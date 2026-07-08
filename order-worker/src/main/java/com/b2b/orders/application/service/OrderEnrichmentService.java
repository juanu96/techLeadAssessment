package com.b2b.orders.application.service;

import com.b2b.orders.application.port.in.CalculateOrderTaxesUseCase;
import com.b2b.orders.application.port.in.EnrichOrderUseCase;
import com.b2b.orders.application.port.out.ClientCachePort;
import com.b2b.orders.application.port.out.ClientCatalogPort;
import com.b2b.orders.application.port.out.ProductCachePort;
import com.b2b.orders.application.port.out.ProductCatalogPort;
import com.b2b.orders.domain.model.Client;
import com.b2b.orders.domain.model.EnrichedOrder;
import com.b2b.orders.domain.model.OrderItem;
import com.b2b.orders.domain.model.OrderLine;
import com.b2b.orders.domain.model.OrderStatus;
import com.b2b.orders.domain.model.PendingOrder;
import com.b2b.orders.domain.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class OrderEnrichmentService implements EnrichOrderUseCase {

    private final ProductCatalogPort productCatalog;
    private final ClientCatalogPort clientCatalog;
    private final ProductCachePort productCache;
    private final ClientCachePort clientCache;
    private final CalculateOrderTaxesUseCase taxService;
    private final String currency;
    private final Clock clock;

    public OrderEnrichmentService(
            ProductCatalogPort productCatalog,
            ClientCatalogPort clientCatalog,
            ProductCachePort productCache,
            ClientCachePort clientCache,
            CalculateOrderTaxesUseCase taxService,
            String currency,
            Clock clock
    ) {
        this.productCatalog = productCatalog;
        this.clientCatalog = clientCatalog;
        this.productCache = productCache;
        this.clientCache = clientCache;
        this.taxService = taxService;
        this.currency = currency;
        this.clock = clock;
    }

    @Override
    public Mono<EnrichedOrder> enrich(PendingOrder order) {
        Mono<Client> client = findClient(order.clientId());
        Mono<Map<String, Product>> products = findProducts(order.items());

        return Mono.zip(client, products)
                .map(data -> buildEnrichedOrder(order, data.getT1(), data.getT2()));
    }

    private Mono<Client> findClient(String clientId) {
        return clientCache.findById(clientId)
                .switchIfEmpty(Mono.defer(() -> clientCatalog.findById(clientId)
                        .flatMap(client -> clientCache.save(client).thenReturn(client))));
    }

    private Mono<Map<String, Product>> findProducts(List<OrderItem> items) {
        return Flux.fromIterable(items)
                .map(OrderItem::productId)
                .distinct()
                .flatMap(this::findProduct)
                .collectMap(Product::productId);
    }

    private Mono<Product> findProduct(String productId) {
        return productCache.findById(productId)
                .switchIfEmpty(Mono.defer(() -> productCatalog.findById(productId)
                        .flatMap(product -> productCache.save(product).thenReturn(product))));
    }

    private EnrichedOrder buildEnrichedOrder(
            PendingOrder order,
            Client client,
            Map<String, Product> products
    ) {
        List<OrderLine> lines = order.items().stream()
                .map(item -> toOrderLine(item, products.get(item.productId())))
                .toList();
        var calculation = taxService.calculate(lines, currency);

        return new EnrichedOrder(
                order.orderId(),
                OrderStatus.PROCESSED,
                client,
                calculation.items(),
                calculation.summary(),
                Instant.now(clock)
        );
    }

    private OrderLine toOrderLine(OrderItem item, Product product) {
        return new OrderLine(
                product.productId(),
                product.name(),
                product.sku(),
                product.taxCategory(),
                item.quantity(),
                item.unitPrice()
        );
    }
}

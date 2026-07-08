package com.b2b.orders.infrastructure.http.dto;

import com.b2b.orders.domain.model.Product;
import com.b2b.orders.domain.model.TaxCategory;

public record ProductResponse(
        String productId,
        String name,
        String sku,
        String category,
        TaxCategory taxCategory,
        String unitOfMeasure
) {
    public Product toDomain() {
        return new Product(productId, name, sku, category, taxCategory, unitOfMeasure);
    }
}

package com.b2b.orders.domain.model;

import java.util.Objects;

public record Product(
        String productId,
        String name,
        String sku,
        String category,
        TaxCategory taxCategory,
        String unitOfMeasure
) {
    public Product {
        requireText(productId, "productId");
        requireText(name, "name");
        requireText(sku, "sku");
        Objects.requireNonNull(taxCategory, "taxCategory is required");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}

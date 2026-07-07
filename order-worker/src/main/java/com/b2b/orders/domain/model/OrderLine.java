package com.b2b.orders.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record OrderLine(
        String productId,
        String name,
        String sku,
        TaxCategory taxCategory,
        BigDecimal quantity,
        BigDecimal unitPrice
) {
    public OrderLine {
        requireText(productId, "productId");
        requireText(name, "name");
        requireText(sku, "sku");
        Objects.requireNonNull(taxCategory, "taxCategory is required");
        Objects.requireNonNull(quantity, "quantity is required");
        Objects.requireNonNull(unitPrice, "unitPrice is required");

        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice cannot be negative");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}

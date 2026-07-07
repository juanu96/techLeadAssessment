package com.b2b.orders.domain.model;

import java.math.BigDecimal;

public record CalculatedOrderLine(
        String productId,
        String name,
        String sku,
        TaxCategory taxCategory,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal lineTotal
) {
}

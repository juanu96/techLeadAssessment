package com.b2b.orders.domain.model;

import java.util.List;

public record OrderTaxCalculation(
        List<CalculatedOrderLine> items,
        OrderSummary summary
) {
    public OrderTaxCalculation {
        items = List.copyOf(items);
    }
}

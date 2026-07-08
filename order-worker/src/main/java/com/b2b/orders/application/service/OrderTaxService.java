package com.b2b.orders.application.service;

import com.b2b.orders.application.port.in.CalculateOrderTaxesUseCase;
import com.b2b.orders.domain.model.CalculatedOrderLine;
import com.b2b.orders.domain.model.OrderLine;
import com.b2b.orders.domain.model.OrderTaxCalculation;
import com.b2b.orders.domain.service.TaxCalculator;

import java.util.List;
import java.util.Objects;

public class OrderTaxService implements CalculateOrderTaxesUseCase {

    private final TaxCalculator taxCalculator;

    public OrderTaxService(TaxCalculator taxCalculator) {
        this.taxCalculator = Objects.requireNonNull(taxCalculator);
    }

    @Override
    public OrderTaxCalculation calculate(List<OrderLine> lines, String currency) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency must use a three-letter ISO code");
        }

        List<CalculatedOrderLine> calculatedLines = lines.stream()
                .map(taxCalculator::calculate)
                .toList();

        return new OrderTaxCalculation(
                calculatedLines,
                taxCalculator.summarize(calculatedLines, currency)
        );
    }
}

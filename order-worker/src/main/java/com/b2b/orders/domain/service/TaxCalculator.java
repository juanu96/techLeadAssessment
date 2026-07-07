package com.b2b.orders.domain.service;

import com.b2b.orders.domain.model.CalculatedOrderLine;
import com.b2b.orders.domain.model.OrderLine;
import com.b2b.orders.domain.model.OrderSummary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

public class TaxCalculator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public CalculatedOrderLine calculate(OrderLine line) {
        BigDecimal subtotal = money(line.quantity().multiply(line.unitPrice()));
        BigDecimal taxRate = line.taxCategory().rate();
        BigDecimal taxAmount = money(subtotal.multiply(taxRate));
        BigDecimal lineTotal = money(subtotal.add(taxAmount));

        return new CalculatedOrderLine(
                line.productId(),
                line.name(),
                line.sku(),
                line.taxCategory(),
                line.quantity(),
                money(line.unitPrice()),
                subtotal,
                taxRate,
                taxAmount,
                lineTotal
        );
    }

    public OrderSummary summarize(List<CalculatedOrderLine> lines, String currency) {
        BigDecimal subtotal = sum(lines, CalculatedOrderLine::subtotal);
        BigDecimal totalTax = sum(lines, CalculatedOrderLine::taxAmount);
        BigDecimal grandTotal = sum(lines, CalculatedOrderLine::lineTotal);

        return new OrderSummary(subtotal, totalTax, grandTotal, currency);
    }

    private BigDecimal sum(
            List<CalculatedOrderLine> lines,
            Function<CalculatedOrderLine, BigDecimal> amount
    ) {
        return money(lines.stream()
                .map(amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING_MODE);
    }
}

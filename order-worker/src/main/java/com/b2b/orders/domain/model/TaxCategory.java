package com.b2b.orders.domain.model;

import java.math.BigDecimal;

public enum TaxCategory {
    GRAVADO("0.19"),
    REDUCIDO("0.05"),
    EXENTO("0.00");

    private final BigDecimal rate;

    TaxCategory(String rate) {
        this.rate = new BigDecimal(rate);
    }

    public BigDecimal rate() {
        return rate;
    }
}

package com.b2b.orders.domain.service;

import com.b2b.orders.domain.model.OrderLine;
import com.b2b.orders.domain.model.TaxCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxCalculatorTest {

    private final TaxCalculator calculator = new TaxCalculator();

    @ParameterizedTest
    @CsvSource({
            "GRAVADO, 0.19, 19.00, 119.00",
            "REDUCIDO, 0.05, 5.00, 105.00",
            "EXENTO, 0.00, 0.00, 100.00"
    })
    void calculatesEachTaxCategory(
            TaxCategory category,
            String expectedRate,
            String expectedTax,
            String expectedTotal
    ) {
        OrderLine line = line(category, "1", "100.00");

        var result = calculator.calculate(line);

        assertThat(result.subtotal()).isEqualByComparingTo("100.00");
        assertThat(result.taxRate()).isEqualByComparingTo(expectedRate);
        assertThat(result.taxAmount()).isEqualByComparingTo(expectedTax);
        assertThat(result.lineTotal()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    void roundsMonetaryValuesToTwoDecimals() {
        OrderLine line = line(TaxCategory.GRAVADO, "3", "0.10");

        var result = calculator.calculate(line);

        assertThat(result.subtotal()).isEqualByComparingTo("0.30");
        assertThat(result.taxAmount()).isEqualByComparingTo("0.06");
        assertThat(result.lineTotal()).isEqualByComparingTo("0.36");
    }

    @Test
    void rejectsInvalidQuantitiesAndPrices() {
        assertThatThrownBy(() -> line(TaxCategory.GRAVADO, "0", "100"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be greater than zero");

        assertThatThrownBy(() -> line(TaxCategory.GRAVADO, "1", "-0.01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unitPrice cannot be negative");
    }

    private OrderLine line(TaxCategory category, String quantity, String unitPrice) {
        return new OrderLine(
                "PRD-001",
                "Producto de prueba",
                "SKU-001",
                category,
                new BigDecimal(quantity),
                new BigDecimal(unitPrice)
        );
    }
}

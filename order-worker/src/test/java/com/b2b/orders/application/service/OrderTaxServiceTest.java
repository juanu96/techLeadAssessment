package com.b2b.orders.application.service;

import com.b2b.orders.domain.model.OrderLine;
import com.b2b.orders.domain.model.TaxCategory;
import com.b2b.orders.domain.service.TaxCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTaxServiceTest {

    private final OrderTaxService service = new OrderTaxService(new TaxCalculator());

    @Test
    void calculatesLinesAndOrderSummary() {
        List<OrderLine> lines = List.of(
                line("PRD-001", TaxCategory.GRAVADO, "24", "3500.00"),
                line("PRD-008", TaxCategory.REDUCIDO, "12", "8200.00")
        );

        var result = service.calculate(lines, "COP");

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().getFirst().subtotal()).isEqualByComparingTo("84000.00");
        assertThat(result.items().getFirst().taxAmount()).isEqualByComparingTo("15960.00");
        assertThat(result.summary().subtotal()).isEqualByComparingTo("182400.00");
        assertThat(result.summary().totalTax()).isEqualByComparingTo("20880.00");
        assertThat(result.summary().grandTotal()).isEqualByComparingTo("203280.00");
        assertThat(result.summary().currency()).isEqualTo("COP");
    }

    @Test
    void rejectsOrdersWithoutItems() {
        assertThatThrownBy(() -> service.calculate(List.of(), "COP"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("order must contain at least one item");
    }

    @Test
    void rejectsInvalidCurrencyCodes() {
        assertThatThrownBy(() -> service.calculate(
                List.of(line("PRD-001", TaxCategory.EXENTO, "1", "10.00")),
                "cop"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currency must use a three-letter ISO code");
    }

    private OrderLine line(String productId, TaxCategory category, String quantity, String unitPrice) {
        return new OrderLine(
                productId,
                "Producto " + productId,
                "SKU-" + productId,
                category,
                new BigDecimal(quantity),
                new BigDecimal(unitPrice)
        );
    }
}

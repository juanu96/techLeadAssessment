package com.b2b.orders.application.port.in;

import com.b2b.orders.domain.model.OrderLine;
import com.b2b.orders.domain.model.OrderTaxCalculation;

import java.util.List;

public interface CalculateOrderTaxesUseCase {

    OrderTaxCalculation calculate(List<OrderLine> lines, String currency);
}

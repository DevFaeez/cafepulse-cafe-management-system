package com.cafemanagement.cafeprediction.report;

import com.cafemanagement.cafeprediction.model.enumoption.SALES_UNIT;

import java.math.BigDecimal;
import java.time.Instant;

public final class SalesEvents {
    private SalesEvents() {}

    public record Payment(Instant paidAt, Instant refundedAt, BigDecimal amount, BigDecimal subtotal,
                          BigDecimal promoDiscount, BigDecimal pointsDiscount) {}

    public record Item(Instant paidAt, Instant refundedAt, Long productId, String productName,
                       SALES_UNIT unitAtOrder, SALES_UNIT currentUnit, int quantity) {
        public SALES_UNIT unit() {
            if (unitAtOrder != null && unitAtOrder != SALES_UNIT.UNCLASSIFIED) return unitAtOrder;
            return currentUnit == null ? SALES_UNIT.UNCLASSIFIED : currentUnit;
        }
    }
}

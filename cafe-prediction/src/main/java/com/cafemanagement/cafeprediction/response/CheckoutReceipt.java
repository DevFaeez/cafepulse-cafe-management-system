package com.cafemanagement.cafeprediction.response;

import com.cafemanagement.cafeprediction.model.Orders;
import com.cafemanagement.cafeprediction.model.enumoption.ORDER_STATUS;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CheckoutReceipt(Long id, String orderSequence, LocalDate businessDate, BigDecimal totalAmount,
                              ORDER_STATUS status, BigDecimal subtotal, BigDecimal promoDiscount,
                              BigDecimal pointsDiscount, Integer pointsRedeemed, String promoCode, Integer pointsEarned) {
    public static CheckoutReceipt of(Orders order) {
        return new CheckoutReceipt(order.getId(), order.getOrderSequence(), order.getBusinessDate(),
                order.getTotalAmount(), order.getStatus(), order.getSubtotal(), order.getPromoDiscount(),
                order.getPointsDiscount(), order.getPointsRedeemed(), order.getPromoCode(), order.getLoyaltyPoint());
    }
}

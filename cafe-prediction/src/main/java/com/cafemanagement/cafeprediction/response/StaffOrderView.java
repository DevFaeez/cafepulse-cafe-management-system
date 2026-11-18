package com.cafemanagement.cafeprediction.response;

import com.cafemanagement.cafeprediction.model.OrderPayment;
import com.cafemanagement.cafeprediction.model.Orders;
import com.cafemanagement.cafeprediction.model.enumoption.ORDER_STATUS;
import com.cafemanagement.cafeprediction.model.enumoption.PAYMENT_METHOD;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StaffOrderView(Long id, String orderSequence, LocalDate businessDate, LocalDateTime createdAt, Instant acceptedAt,
                             ORDER_STATUS status, long revision, BigDecimal totalAmount,
                             Instant preparingAt, String preparingBy, Instant completedAt, String completedBy,
                             List<OrderItemResponse> items, List<Payment> payments, BigDecimal subtotal, BigDecimal promoDiscount,
                             BigDecimal pointsDiscount, Integer pointsRedeemed, String promoCode,
                             Instant closedAt, String closedBy, String closeReason, boolean recipeStockDeducted) {
    public static StaffOrderView of(Orders order, List<OrderPayment> payments) {
        return new StaffOrderView(order.getId(), order.getOrderSequence(), order.getBusinessDate(), order.getCreatedAt(), order.getAcceptedAt(),
                order.getStatus(), order.currentRevision(), order.getTotalAmount(), order.getPreparingAt(),
                order.getPreparingBy(), order.getCompletedAt(), order.getCompletedBy(),
                order.getOrderItems().stream().map(OrderItemMapper::toDTO).toList(),
                payments.stream().map(Payment::of).toList(), order.getSubtotal(), order.getPromoDiscount(),
                order.getPointsDiscount(), order.getPointsRedeemed(), order.getPromoCode(),
                order.getClosedAt(), order.getClosedBy(), order.getCloseReason(), Boolean.TRUE.equals(order.getRecipeStockDeducted()));
    }

    public record Payment(Long id, PAYMENT_METHOD method, BigDecimal amount, Instant recordedAt, String recordedBy,
                          Instant voidedAt, String voidedBy, String voidReason, Instant refundedAt) {
        static Payment of(OrderPayment payment) {
            return new Payment(payment.getId(), payment.getMethod(), payment.getAmount(), payment.getRecordedAt(),
                    payment.getRecordedBy(), payment.getVoidedAt(), payment.getVoidedBy(), payment.getVoidReason(), payment.getRefundedAt());
        }
    }

    public record History(List<StaffOrderView> orders, boolean hasNext) {
    }
}

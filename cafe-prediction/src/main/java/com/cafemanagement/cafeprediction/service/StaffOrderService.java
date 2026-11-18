package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.OrderPayment;
import com.cafemanagement.cafeprediction.model.Orders;
import com.cafemanagement.cafeprediction.model.enumoption.ORDER_STATUS;
import com.cafemanagement.cafeprediction.model.enumoption.PAYMENT_METHOD;
import com.cafemanagement.cafeprediction.repository.OrderPaymentRepository;
import com.cafemanagement.cafeprediction.repository.OrderRepository;
import com.cafemanagement.cafeprediction.response.StaffOrderView;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffOrderService {
    private final OrderRepository orders;
    private final OrderPaymentRepository payments;
    private final DailyOrderSequenceService checkout;
    private final OrderRewards rewards;
    private final InventoryRecipes recipes;

    public StaffOrderService(OrderRepository orders, OrderPaymentRepository payments,
                             DailyOrderSequenceService checkout, OrderRewards rewards, InventoryRecipes recipes) {
        this.orders = orders;
        this.payments = payments;
        this.checkout = checkout;
        this.rewards = rewards;
        this.recipes = recipes;
    }

    @Transactional(readOnly = true)
    public List<StaffOrderView> active() {
        return views(orders.findAllByStatusInOrderByIdAsc(List.of(ORDER_STATUS.ORDER_PENDING, ORDER_STATUS.ORDER_PREPARING)));
    }

    @Transactional(readOnly = true)
    public StaffOrderView.History history(int page) {
        var result = orders.findByStatusInOrderByIdDesc(List.of(ORDER_STATUS.ORDER_COMPLETED, ORDER_STATUS.ORDER_CANCELLED,
                ORDER_STATUS.ORDER_REFUNDED), PageRequest.of(page, 20));
        return new StaffOrderView.History(views(result.getContent()), result.hasNext());
    }

    @Transactional
    public void recordPayment(long id, long revision, PAYMENT_METHOD method, BigDecimal amount, String actor) {
        checkout.lockCheckout();
        Orders order = lockedOrder(id, revision);
        if (order.getStatus() != ORDER_STATUS.ORDER_PENDING && order.getStatus() != ORDER_STATUS.ORDER_PREPARING) {
            throw conflict("This order is already closed.");
        }
        if (payments.findByOrderIdAndVoidedAtIsNullAndRefundedAtIsNull(id).isPresent()) {
            throw conflict("Payment has already been recorded.");
        }
        if (amount.compareTo(order.getTotalAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The payment amount must match the order total.");
        }
        if ((amount.signum() == 0) != (method == PAYMENT_METHOD.NO_CHARGE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The payment method doesn't match the amount.");
        }
        payments.save(new OrderPayment(order, method, order.getTotalAmount(), actor, Instant.now()));
        rewards.earn(order, actor);
        order.advanceRevision();
    }

    @Transactional
    public void voidPayment(long id, long revision, long paymentId, String reason, String actor) {
        checkout.lockCheckout();
        Orders order = lockedOrder(id, revision);
        requirePending(order);
        OrderPayment payment = payments.findByOrderIdAndVoidedAtIsNullAndRefundedAtIsNull(id)
                .filter(record -> record.getId() == paymentId)
                .orElseThrow(() -> conflict("This payment record has changed."));
        payment.voidRecording(actor, reason.strip(), Instant.now());
        rewards.reversePayment(order, actor);
        order.advanceRevision();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void changeStatus(long id, long revision, ORDER_STATUS next, String actor) {
        if (next == ORDER_STATUS.ORDER_PREPARING) checkout.lockCheckout();
        Orders order = lockedOrder(id, revision);
        Instant now = Instant.now();
        if (order.getStatus() == ORDER_STATUS.ORDER_PENDING && next == ORDER_STATUS.ORDER_PREPARING) {
            if (payments.findByOrderIdAndVoidedAtIsNullAndRefundedAtIsNull(id).isEmpty()) {
                throw conflict("Record payment before starting preparation.");
            }
            recipes.deduct(order, actor);
            order.setPreparingAt(now);
            order.setPreparingBy(actor);
        } else if (order.getStatus() == ORDER_STATUS.ORDER_PREPARING && next == ORDER_STATUS.ORDER_COMPLETED) {
            if (payments.findByOrderIdAndVoidedAtIsNullAndRefundedAtIsNull(id).isEmpty()) {
                throw conflict("Record payment before marking the order as ready.");
            }
            order.setCompletedAt(now);
            order.setCompletedBy(actor);
        } else {
            throw conflict("This status change isn't allowed.");
        }

        order.setStatus(next);
        order.advanceRevision();
    }

    @Transactional
    public void close(long id, long revision, boolean refund, boolean restoreStock, String reason, String actor) {
        checkout.lockCheckout();
        Orders order = lockedOrder(id, revision);
        var activePayment = payments.findByOrderIdAndVoidedAtIsNullAndRefundedAtIsNull(id);
        if (restoreStock) {
            if (!refund || order.getStatus() != ORDER_STATUS.ORDER_PREPARING || activePayment.isEmpty()
                    || !Boolean.TRUE.equals(order.getRecipeStockDeducted())) {
                throw conflict("Ingredients can be returned only when refunding an order in preparation and stock has already been deducted.");
            }
            recipes.restore(order, actor);
        }
        if (refund) {
            if (order.getStatus() == ORDER_STATUS.ORDER_CANCELLED || order.getStatus() == ORDER_STATUS.ORDER_REFUNDED
                    || activePayment.isEmpty()) {
                throw conflict("This order has no active payment to refund.");
            }
            OrderPayment payment = activePayment.get();
            order.setStatus(ORDER_STATUS.ORDER_REFUNDED);
            payment.refund();
        } else {
            requirePending(order);
            if (activePayment.isPresent()) throw conflict("Record a refund for this paid order.");
            order.setStatus(ORDER_STATUS.ORDER_CANCELLED);
        }
        order.setClosedAt(Instant.now());
        order.setClosedBy(actor);
        order.setCloseReason(reason.strip());
        rewards.restoreOnClose(order, refund, actor);
        order.advanceRevision();
    }

    private Orders lockedOrder(long id, long revision) {
        Orders order = orders.lockById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
        if (order.currentRevision() != revision) {
            throw conflict("This order has changed. Check its latest status before trying again.");
        }
        return order;
    }

    private void requirePending(Orders order) {
        if (order.getStatus() != ORDER_STATUS.ORDER_PENDING) {
            throw conflict("Payment can only be corrected while an order is waiting.");
        }
    }

    private List<StaffOrderView> views(List<Orders> items) {
        if (items.isEmpty()) return List.of();
        var byOrder = payments.findAllByOrderIdInOrderByIdDesc(items.stream().map(Orders::getId).toList())
                .stream().collect(Collectors.groupingBy(payment -> payment.getOrder().getId()));
        return items.stream().map(order -> StaffOrderView.of(order, byOrder.getOrDefault(order.getId(), List.of()))).toList();
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}

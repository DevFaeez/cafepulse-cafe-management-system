package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.Customer;
import com.cafemanagement.cafeprediction.model.LoyaltyMovement;
import com.cafemanagement.cafeprediction.model.LoyaltyPoint;
import com.cafemanagement.cafeprediction.model.Orders;
import com.cafemanagement.cafeprediction.repository.LoyaltyMovementRepository;
import com.cafemanagement.cafeprediction.repository.LoyaltyPointRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.cafemanagement.cafeprediction.model.LoyaltyMovement.Reason.*;

@Service
public class OrderRewards {
    public static final int POINTS_PER_RINGGIT = 10;
    private final LoyaltyPointRepository balances;
    private final LoyaltyMovementRepository movements;

    public OrderRewards(LoyaltyPointRepository balances, LoyaltyMovementRepository movements) {
        this.balances = balances;
        this.movements = movements;
    }

    public int available(Customer customer) {
        return Math.max(0, balance(customer));
    }

    public int balance(Customer customer) {
        if (customer == null) return 0;
        return balances.findByCustomer_Id(customer.getId()).map(LoyaltyPoint::netPoints).orElse(0);
    }

    public History history(Customer customer) {
        return new History(available(customer), balance(customer),
                movements.findByCustomerIdOrderByIdDesc(customer.getId(), PageRequest.of(0, 50))
                        .stream().map(entry -> new Movement(entry.getRecordedAt(), entry.getReason(), entry.getPoints(),
                                entry.getBalanceAfter(), entry.getOrder().getOrderSequence(), entry.getActor())).toList());
    }

    public int earned(BigDecimal amount) {
        if (amount.signum() == 0) return 0;
        if (amount.compareTo(BigDecimal.valueOf(30)) <= 0) return 15;
        if (amount.compareTo(BigDecimal.valueOf(50)) <= 0) return 35;
        if (amount.compareTo(BigDecimal.valueOf(80)) <= 0) return 70;
        return 70 + amount.subtract(BigDecimal.valueOf(80)).divideToIntegralValue(BigDecimal.valueOf(2)).intValueExact();
    }

    // The checkout lock is held by callers of the mutation methods.
    public void reserve(Orders order, String actor) {
        adjust(order, -order.getPointsRedeemed(), REDEEMED, actor);
    }

    public void earn(Orders order, String actor) {
        if (order.getSubtotal() != null) adjust(order, order.getLoyaltyPoint(), EARNED, actor);
    }

    public void reversePayment(Orders order, String actor) {
        if (order.getSubtotal() != null) adjust(order, -order.getLoyaltyPoint(), EARNING_REVERSED, actor);
    }

    public void restoreOnClose(Orders order, boolean paid, String actor) {
        if (order.getSubtotal() != null) {
            if (paid) reversePayment(order, actor);
            adjust(order, order.getPointsRedeemed(), REDEMPTION_RESTORED, actor);
        } else if (order.getCheckoutKey() != null) {
            adjust(order, -order.getLoyaltyPoint(), LEGACY_EARNING_REVERSED, actor);
        }
    }

    private void adjust(Orders order, int change, LoyaltyMovement.Reason reason, String actor) {
        if (change == 0) return;
        var balance = balances.findByCustomer_Id(order.getCustomer().getId()).orElseGet(() -> {
            LoyaltyPoint entry = new LoyaltyPoint();
            entry.setCustomer(order.getCustomer());
            return entry;
        });
        int updated = Math.addExact(balance.netPoints(), change);
        balance.setPointsBalance(Math.max(0, updated));
        balance.setPointsDebt(updated < 0 ? Math.negateExact(updated) : 0);
        balances.save(balance);
        movements.save(new LoyaltyMovement(order, change, updated, reason, actor));
    }

    public record Movement(Instant time, LoyaltyMovement.Reason reason, int points, int balanceAfter, String queue, String actor) {}
    public record History(int available, int balance, List<Movement> movements) {}
}

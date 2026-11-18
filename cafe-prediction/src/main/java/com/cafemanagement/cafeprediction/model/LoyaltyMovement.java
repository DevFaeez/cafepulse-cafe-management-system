package com.cafemanagement.cafeprediction.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoyaltyMovement {
    public enum Reason { REDEEMED, EARNED, EARNING_REVERSED, REDEMPTION_RESTORED, LEGACY_EARNING_REVERSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private int balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Reason reason;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 100)
    private String actor;

    public LoyaltyMovement(Orders order, int points, int balanceAfter, Reason reason, String actor) {
        this.order = order;
        this.customer = order.getCustomer();
        this.points = points;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.actor = actor;
        this.recordedAt = Instant.now();
    }
}

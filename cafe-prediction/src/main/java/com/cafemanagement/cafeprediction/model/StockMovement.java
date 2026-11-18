package com.cafemanagement.cafeprediction.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"stock_id", "request_id"}),
        @UniqueConstraint(columnNames = {"stock_id", "order_id", "kind"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {
    public enum Kind { OPENING, RECEIPT, USAGE, ADJUSTMENT, WASTE, ORDER_USAGE, ORDER_RETURN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Orders order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal delta;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal balanceAfter;

    @Column(precision = 12, scale = 3)
    private BigDecimal requestedQuantity;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(nullable = false)
    private Instant recordedAt;

    public StockMovement(Stock stock, Orders order, Kind kind, BigDecimal delta, BigDecimal balanceAfter,
                         BigDecimal requestedQuantity, String requestId, String reason, String actor) {
        this.stock = stock;
        this.order = order;
        this.kind = kind;
        this.delta = delta;
        this.balanceAfter = balanceAfter;
        this.requestedQuantity = requestedQuantity;
        this.requestId = requestId;
        this.reason = reason;
        this.actor = actor;
        this.recordedAt = Instant.now();
    }
}

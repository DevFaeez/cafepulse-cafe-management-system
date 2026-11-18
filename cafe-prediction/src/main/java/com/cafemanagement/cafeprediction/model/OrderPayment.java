package com.cafemanagement.cafeprediction.model;

import com.cafemanagement.cafeprediction.model.enumoption.PAYMENT_METHOD;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(indexes = {@Index(name = "idx_payment_recorded_at", columnList = "recorded_at"),
        @Index(name = "idx_payment_refunded_at", columnList = "refunded_at")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Orders order;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 12)
    private PAYMENT_METHOD method;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 100)
    private String recordedBy;

    private Instant voidedAt;

    private Instant refundedAt;

    @Column(length = 100)
    private String voidedBy;

    @Column(length = 200)
    private String voidReason;

    public OrderPayment(Orders order, PAYMENT_METHOD method, BigDecimal amount, String actor, Instant now) {
        this.order = order;
        this.method = method;
        this.amount = amount;
        this.recordedBy = actor;
        this.recordedAt = now;
    }

    public void refund() {
        this.refundedAt = Instant.now();
    }

    public void voidRecording(String actor, String reason, Instant now) {
        this.voidedBy = actor;
        this.voidReason = reason;
        this.voidedAt = now;
    }
}

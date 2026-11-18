package com.cafemanagement.cafeprediction.model;

import com.cafemanagement.cafeprediction.model.enumoption.ORDER_STATUS;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = @Index(name = "idx_orders_status_closed_at", columnList = "status, closed_at"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties("phoneNumber")
    private Customer customer;

    @Column(nullable = false)
    @NotNull(message = "loyalty Point is required")
    @Min(0)
    private Integer loyaltyPoint; // it for loyalty point

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ORDER_STATUS status = ORDER_STATUS.ORDER_PENDING;

    @Column(nullable = false, precision = 10, scale = 2) //precision total number and scale bilangan perpuluhan
    @NotNull(message = "Enter a price.")
    @DecimalMin("0.00")
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal promoDiscount;

    @Column(precision = 10, scale = 2)
    private BigDecimal pointsDiscount;

    private Integer pointsRedeemed;

    @Column(length = 24)
    private String promoCode;

    private Instant closedAt;
    @Column(length = 100)
    private String closedBy;
    @Column(length = 200)
    private String closeReason;

    private Boolean recipeStockDeducted;

    private String orderSequence;

    private LocalDate businessDate;
    private Instant acceptedAt;

    @JsonIgnore
    private Long workflowRevision;

    private Instant preparingAt;
    @Column(length = 100)
    private String preparingBy;

    private Instant completedAt;
    @Column(length = 100)
    private String completedBy;

    public long currentRevision() {
        return workflowRevision == null ? 0 : workflowRevision;
    }

    public void advanceRevision() {
        workflowRevision = currentRevision() + 1;
    }

    @JsonIgnore
    @Column(unique = true, length = 64)
    private String checkoutKey;

    @JsonIgnore
    @Column(length = 64)
    private String checkoutFingerprint;

    @JsonIgnore
    @BatchSize(size = 32)
    @OneToMany(mappedBy = "order", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime complatedAt;
}

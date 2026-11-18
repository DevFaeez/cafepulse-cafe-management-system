package com.cafemanagement.cafeprediction.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    @NotNull(message = "Points balance is required")
    @Min(0)
    private Integer pointsBalance = 0;

    @Min(0)
    private Integer pointsDebt;

    public int netPoints() {
        return pointsBalance - (pointsDebt == null ? 0 : pointsDebt);
    }

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime lastUpdated;
}

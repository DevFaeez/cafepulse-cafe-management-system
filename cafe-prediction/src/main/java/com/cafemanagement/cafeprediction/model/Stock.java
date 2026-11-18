package com.cafemanagement.cafeprediction.model;

import com.cafemanagement.cafeprediction.model.enumoption.STOCK_STATUS;
import com.cafemanagement.cafeprediction.model.enumoption.STOCK_UNIT;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "Stock name is required")
    private String stockName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private STOCK_STATUS status = STOCK_STATUS.STOCK_ENOUGH; // default

    @Enumerated(EnumType.STRING)
    private STOCK_UNIT unit;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(precision = 12, scale = 3)
    private BigDecimal minimum;

    private Long revision;

    public boolean isConfigured() {
        return unit != null && quantity != null && minimum != null;
    }

    public void advanceRevision() {
        revision = revision == null ? 1 : revision + 1;
    }

    public void updateStatus() {
        status = quantity.signum() == 0 ? STOCK_STATUS.STOCK_OUT_OF_STOCK
                : quantity.compareTo(minimum) <= 0 ? STOCK_STATUS.STOCK_LOW_STOCK : STOCK_STATUS.STOCK_ENOUGH;
    }

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

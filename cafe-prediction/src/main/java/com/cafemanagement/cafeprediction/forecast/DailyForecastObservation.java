package com.cafemanagement.cafeprediction.forecast;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyForecastObservation {
    @Id
    private LocalDate businessDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String scenarioJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String productsJson;

    @Column(nullable = false)
    private boolean closed;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false, length = 100)
    private String updatedBy;

    public DailyForecastObservation(LocalDate businessDate, String scenarioJson, String productsJson,
                                    boolean closed, String updatedBy) {
        this.businessDate = businessDate;
        this.scenarioJson = scenarioJson;
        this.productsJson = productsJson;
        this.closed = closed;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }
}

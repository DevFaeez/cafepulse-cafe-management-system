package com.cafemanagement.cafeprediction.forecast;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DailyForecastObservationRepository extends JpaRepository<DailyForecastObservation, LocalDate> {
    List<DailyForecastObservation> findByBusinessDateBetweenOrderByBusinessDateAsc(LocalDate from, LocalDate to);
}

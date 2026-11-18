package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.Promo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromoRepository extends JpaRepository<Promo, Long> {
    List<Promo> findAllByOrderByIdDesc();
    List<Promo> findByCodeIsNotNullAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanOrderByCode(
            LocalDateTime now, LocalDateTime nowAgain);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<Promo> findByCode(String code);
}

package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.DailyOrderSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyOrderSequenceRepository extends JpaRepository<DailyOrderSequence, Long> {
    Optional<DailyOrderSequence> findFirstByDateOrderBySequenceDescIdDesc(LocalDate date);
}

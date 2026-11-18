package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.LoyaltyMovement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyMovementRepository extends JpaRepository<LoyaltyMovement, Long> {
    List<LoyaltyMovement> findByCustomerIdOrderByIdDesc(Long customerId, Pageable page);
}

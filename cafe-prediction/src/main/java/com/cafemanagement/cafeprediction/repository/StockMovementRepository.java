package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.StockMovement;
import com.cafemanagement.cafeprediction.model.StockMovement.Kind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    Optional<StockMovement> findByStockIdAndRequestId(Long stockId, String requestId);
    Slice<StockMovement> findByStockIdOrderByIdDesc(Long stockId, Pageable page);
    List<StockMovement> findAllByOrderIdAndKind(Long orderId, Kind kind);
}

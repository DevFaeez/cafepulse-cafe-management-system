package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.Stock;
import com.cafemanagement.cafeprediction.model.enumoption.STOCK_STATUS;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findAllByOrderByStockNameAscIdAsc();
    List<Stock> findAllByQuantityIsNotNullAndStatusInOrderByStockNameAsc(List<STOCK_STATUS> status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.id = :id")
    Optional<Stock> lockById(@Param("id") Long id);
}

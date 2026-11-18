package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.Orders;
import com.cafemanagement.cafeprediction.model.enumoption.ORDER_STATUS;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    @Query("""
            select o.closedAt from Orders o
            where o.status = com.cafemanagement.cafeprediction.model.enumoption.ORDER_STATUS.ORDER_CANCELLED
                and o.closedAt >= :start and o.closedAt < :end
            """)
    List<Instant> salesCancellations(@Param("start") Instant start, @Param("end") Instant end);

    Optional<Orders> findByCheckoutKey(String checkoutKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orders o where o.id = :id")
    Optional<Orders> lockById(@Param("id") long id);

    List<Orders> findAllByStatusInOrderByIdAsc(List<ORDER_STATUS> statuses);
    Slice<Orders> findByStatusInOrderByIdDesc(List<ORDER_STATUS> statuses, Pageable page);

    // select order by status
    List<Orders> findAllByStatus(ORDER_STATUS orderStatus);

    // select all order pending and prepering
    List<Orders> findAllByStatusIn(List<ORDER_STATUS> statuses);
}

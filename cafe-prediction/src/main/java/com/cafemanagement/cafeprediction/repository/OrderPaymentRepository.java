package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.OrderPayment;
import com.cafemanagement.cafeprediction.report.SalesEvents;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {
    String SALES_RANGE = """
            where p.voidedAt is null
            and ((p.recordedAt >= :start and p.recordedAt < :end) or (p.refundedAt >= :start and p.refundedAt < :end))
            """;

    @Query("""
            select new com.cafemanagement.cafeprediction.report.SalesEvents$Payment(
                p.recordedAt, p.refundedAt, p.amount, o.subtotal, o.promoDiscount, o.pointsDiscount)
            from OrderPayment p join p.order o
            """ + SALES_RANGE)
    List<SalesEvents.Payment> salesPayments(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            select new com.cafemanagement.cafeprediction.report.SalesEvents$Item(
                p.recordedAt, p.refundedAt, product.id, product.name, i.salesUnitSnapshot, product.salesUnit, i.quantity)
            from OrderPayment p join p.order o join o.orderItems i join i.product product
            """ + SALES_RANGE)
    List<SalesEvents.Item> salesItems(@Param("start") Instant start, @Param("end") Instant end);

    Optional<OrderPayment> findByOrderIdAndVoidedAtIsNullAndRefundedAtIsNull(Long orderId);
    List<OrderPayment> findAllByOrderIdInOrderByIdDesc(List<Long> orderIds);
}

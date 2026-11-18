package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.Customer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomerLoyaltyRepository extends Repository<Customer, Long> {
    String HAS_PHONE = "c.phone_number IS NOT NULL AND TRIM(c.phone_number) <> ''";
    String MATCH_PHONE = """
            AND (CASE WHEN REGEXP_REPLACE(c.phone_number, '[ ()-]', '') LIKE '+60%'
                  THEN CONCAT('0', SUBSTRING(REGEXP_REPLACE(c.phone_number, '[ ()-]', ''), 4))
                  ELSE REGEXP_REPLACE(c.phone_number, '[ ()-]', '') END) LIKE CONCAT('%', :phone, '%')
            """;
    String AVAILABLE = "GREATEST(COALESCE(l.points_balance, 0) - COALESCE(l.points_debt, 0), 0)";
    String PURCHASES = """
            SELECT o.customer_id, COUNT(DISTINCT o.id) AS paid_orders, MAX(p.recorded_at) AS last_paid
            FROM order_payment p JOIN orders o ON o.id = p.order_id
            WHERE p.voided_at IS NULL AND p.refunded_at IS NULL
            GROUP BY o.customer_id
            """;

    @Query(value = "SELECT COUNT(*) FROM customer c WHERE " + HAS_PHONE + " " + MATCH_PHONE, nativeQuery = true)
    long countMatching(@Param("phone") String phone);

    @Query(value = """
            WITH selected AS (
                SELECT c.id, c.phone_number, c.created_at
                FROM customer c
                WHERE
            """ + HAS_PHONE + " " + MATCH_PHONE + """
                ORDER BY c.id DESC LIMIT :limit OFFSET :offset
            )
            SELECT c.id AS id, c.phone_number AS phone, c.created_at AS joinedAt,
                COUNT(DISTINCT p.order_id) AS paidOrders,
            """ + AVAILABLE + """
                AS availablePoints
            FROM selected c
            LEFT JOIN loyalty_point l ON l.customer_id = c.id
            LEFT JOIN orders o ON o.customer_id = c.id
            LEFT JOIN order_payment p ON p.order_id = o.id AND p.voided_at IS NULL AND p.refunded_at IS NULL
            GROUP BY c.id, c.phone_number, c.created_at, l.points_balance, l.points_debt
            ORDER BY c.id DESC
            """, nativeQuery = true)
    List<Contact> directory(@Param("phone") String phone, @Param("limit") int limit, @Param("offset") long offset);

    @Query(value = "WITH purchases AS (" + PURCHASES + """
            )
            SELECT COUNT(*) AS totalCustomers,
                COALESCE(SUM(CASE WHEN p.paid_orders = 1 THEN 1 ELSE 0 END), 0) AS singlePurchase,
                COALESCE(SUM(CASE WHEN p.paid_orders >= 2 THEN 1 ELSE 0 END), 0) AS returningCustomers,
                COALESCE(SUM(CASE WHEN p.last_paid >= :activeSince THEN 1 ELSE 0 END), 0) AS activeCustomers,
                COALESCE(SUM(
            """ + AVAILABLE + """
                ), 0) AS totalPoints,
                COALESCE(SUM(CASE WHEN
            """ + AVAILABLE + """
                BETWEEN 1 AND 99 THEN 1 ELSE 0 END), 0) AS pointsUnder100,
                COALESCE(SUM(CASE WHEN
            """ + AVAILABLE + """
                >= 100 THEN 1 ELSE 0 END), 0) AS pointsAtLeast100
            FROM customer c
            LEFT JOIN loyalty_point l ON l.customer_id = c.id
            LEFT JOIN purchases p ON p.customer_id = c.id
            WHERE
            """ + HAS_PHONE, nativeQuery = true)
    Overview overview(@Param("activeSince") Instant activeSince);

    @Query(value = """
            SELECT DATE_FORMAT(c.created_at, '%Y-%m') AS monthKey, COUNT(*) AS total
            FROM customer c
            WHERE c.created_at >= :from AND c.created_at < :until AND
            """ + HAS_PHONE + " GROUP BY monthKey ORDER BY monthKey", nativeQuery = true)
    List<MonthCount> registrations(@Param("from") LocalDateTime from, @Param("until") LocalDateTime until);

    interface Contact {
        Long getId();
        String getPhone();
        LocalDateTime getJoinedAt();
        long getPaidOrders();
        int getAvailablePoints();
    }

    interface Overview {
        long getTotalCustomers();
        long getSinglePurchase();
        long getReturningCustomers();
        long getActiveCustomers();
        long getTotalPoints();
        long getPointsUnder100();
        long getPointsAtLeast100();
    }

    interface MonthCount {
        String getMonthKey();
        long getTotal();
    }
}

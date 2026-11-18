package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findFirstByPhoneNumberOrderById(String phoneNumber);

    @Query(value = """
            SELECT c.* FROM customer c
            WHERE CASE
                WHEN REGEXP_REPLACE(c.phone_number, '[ ()-]', '') LIKE '+60%'
                THEN CONCAT('0', SUBSTRING(REGEXP_REPLACE(c.phone_number, '[ ()-]', ''), 4))
                ELSE REGEXP_REPLACE(c.phone_number, '[ ()-]', '')
            END = :phone
            ORDER BY c.id LIMIT 1
            """, nativeQuery = true)
    Optional<Customer> findLegacyPhone(@Param("phone") String phone);
}

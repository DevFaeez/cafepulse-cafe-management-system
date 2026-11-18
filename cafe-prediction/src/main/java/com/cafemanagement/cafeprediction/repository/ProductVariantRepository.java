package com.cafemanagement.cafeprediction.repository;

import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.cafemanagement.cafeprediction.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select x from ProductVariant x where x.id = :id")
    Optional<ProductVariant> findForCheckout(@Param("id") Long id);


    // select product varaint by product id
    List<ProductVariant> findByProductId(Long productId);

//    List<ProductVariant> findBy

}

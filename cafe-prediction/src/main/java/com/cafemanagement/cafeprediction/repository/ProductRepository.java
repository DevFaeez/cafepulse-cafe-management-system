package com.cafemanagement.cafeprediction.repository;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.cafemanagement.cafeprediction.model.Product;
import com.cafemanagement.cafeprediction.model.enumoption.PRODUCTS_STATUS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select x from Product x where x.id = :id")
    Optional<Product> findForCheckout(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> lockForManagement(@Param("id") Long id);


    //find all product available query
    List<Product> findAllByStatus(PRODUCTS_STATUS productsStatus);

    //search by name
//    @Query("SELECT p FROM Product p WHERE p.name LIKE %:keyword%")
//    List<Product> searchProduct(@Param("keyword") String keyword);

    List<Product> findByNameContaining(String keyword);

}

package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.OrderItem;
import com.cafemanagement.cafeprediction.model.Orders;
import org.hibernate.query.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

     List<OrderItem> findAllByOrder_Id(Long id);

}

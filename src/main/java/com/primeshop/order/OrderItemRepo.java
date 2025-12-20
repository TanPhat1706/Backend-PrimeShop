package com.primeshop.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.primeshop.product.Product;

public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {

    // Lấy danh sách sản phẩm mà user đã từng mua
    @Query("SELECT oi.product FROM OrderItem oi WHERE oi.order.user.id = :userId")
    List<Product> findProductsByUserId(@Param("userId") Long userId);
    List<OrderItem> findAllByOrderUserId(Long userId);
}

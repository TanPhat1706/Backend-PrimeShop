package com.primeshop.order;

import java.math.BigDecimal;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal totalPrice;

    public OrderItemResponse(OrderItem orderItem) {
        this.productId = orderItem.getProduct().getId();
        this.productName = orderItem.getProduct().getName();
        this.quantity = orderItem.getQuantity();
        this.totalPrice = orderItem.getTotalPrice();
    }
}

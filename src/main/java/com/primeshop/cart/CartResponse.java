package com.primeshop.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class CartResponse {
    private Long id;
    private BigDecimal totalPrice;
    private List<CartItemResponse> items;

    public CartResponse(Cart cart) {
        this.id = cart.getId();
        this.items = cart.getCartItems().stream()
                .map(CartItemResponse::new)
                .collect(Collectors.toList());
        this.totalPrice = cart.getTotalPrice();
    }
}

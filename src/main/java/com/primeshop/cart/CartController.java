package com.primeshop.cart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:5173")
public class CartController {
    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addToCart(request));
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeFromCart(@RequestBody CartItemRequest request) {
        cartService.removeFromCart(request);
        return ResponseEntity.ok("Xóa sản phẩm khỏi giỏ hàng thành công!");
    }

    @GetMapping
    public ResponseEntity<?> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @GetMapping("/count")
    public ResponseEntity<?> getCartItemCount() {
        return ResponseEntity.ok(cartService.getCartItemCount());
    }
}

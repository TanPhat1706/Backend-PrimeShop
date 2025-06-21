package com.primeshop.order;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping("/get")
    public ResponseEntity<List<OrderResponse>> getOrder() {
        return ResponseEntity.ok(orderService.getOrdersByUser());
    }

    @GetMapping("/all-orders")
    public ResponseEntity<?> getAllOrders(@ModelAttribute OrderFilterRequest request) {
        return ResponseEntity.ok(orderService.searchOrders(request));
    }

    @GetMapping("/count")
    public ResponseEntity<?> countOrder() {
        return ResponseEntity.ok(orderService.countOrder());
    }
}

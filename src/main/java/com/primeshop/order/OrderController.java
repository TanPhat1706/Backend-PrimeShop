package com.primeshop.order;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {
    @Autowired
    private OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

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

    @PutMapping("/update-status")
    public ResponseEntity<?> updateStatus(
            @RequestParam Long id, 
            @RequestParam String status) {

        try {
            boolean updated = orderService.updateOrderStatus(id, status);

            if (updated) {
                return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Không tìm thấy đơn hàng hoặc trạng thái không hợp lệ"));
            }
        } catch (Exception e) {
            // Log lỗi chi tiết ở đây nếu cần
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống khi cập nhật trạng thái"));
        }
    }

}

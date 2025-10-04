package com.primeshop.payment.momo;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.primeshop.config.MoMoConfig;
import com.primeshop.order.Order;
import com.primeshop.order.OrderRepo;
import com.primeshop.order.OrderService;
import com.primeshop.order.OrderStatus;
import com.primeshop.utils.MoMoSignatureUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payment/momo")
@CrossOrigin(origins =  "http://locahost:5173")
@RequiredArgsConstructor
public class MoMoPaymentController {
    
    private final MoMoPaymentService moMoPaymentService;
    private final OrderService orderService;
    private final MoMoConfig config;

    @Autowired
    private final OrderRepo orderRepo;

    @PostMapping("/create")
    public MoMoPaymentResponse createPayment(@RequestBody MoMoRequest request) throws Exception {
        Optional<Order> orderOptional = orderRepo.findById(request.getOrderId());
        Order order = orderOptional.get();
        
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            String orderId = order.getId().toString() + "-" + System.currentTimeMillis();
            Long amount = order.getTotalAmount().longValue();
            String orderInfo = "Thanh toán đơn hàng " + orderId + " của " + order.getFullName();
            return moMoPaymentService.createPayment(orderId, amount, orderInfo);
        } else {
            throw new IllegalStateException("Đơn hàng của bạn chưa đạt điều kiện thanh toán!");
        }
    }

    @GetMapping("/return")
    public void handleReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        // Debug logging to see what MoMo is sending
        System.out.println("=== MoMo Return Parameters ===");
        params.forEach((key, value) -> System.out.println(key + " = " + value));
        
        // Fix orderId parsing - extract the original orderId from the MoMo orderId
        String momoOrderId = params.get("orderId");
        if (momoOrderId == null) {
            response.sendRedirect("http://localhost:5173/payment/fail?reason=missing_order_id");
            return;
        }
        
        // Extract original orderId (before the timestamp suffix)
        Long orderId;
        try {
            if (momoOrderId.contains("-")) {
                orderId = Long.parseLong(momoOrderId.split("-")[0]);
            } else {
                orderId = Long.parseLong(momoOrderId);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing orderId: " + momoOrderId);
            response.sendRedirect("http://localhost:5173/payment/fail?reason=invalid_order_id");
            return;
        }
        
        String resultCode = params.get("resultCode");
        System.out.println("Extracted orderId: " + orderId);
        System.out.println("Result code: " + resultCode);

        // Validate signature
        boolean isValidSignature = MoMoSignatureUtils.isValidSignature(params, config.getSecretKey(), config.getAccessKey());
        System.out.println("Signature validation result: " + isValidSignature);
        
        if (!isValidSignature) {
            response.sendRedirect("http://localhost:5173/payment/fail?reason=invalid_signature");
            return;
        }

        if ("0".equals(resultCode)) {
            orderService.updateOrderStatus(orderId, OrderStatus.PAID);
            response.sendRedirect("http://localhost:5173/payment/success?orderId=" + orderId);
        } else {
            orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_FAILED);
            response.sendRedirect("http://localhost:5173/payment/fail?orderId=" + orderId);
        }
    }

    @PostMapping("/notify")
    public void handleNotify(@RequestBody Map<String, String> params, HttpServletResponse response) throws IOException {
        // Debug logging for IPN
        System.out.println("=== MoMo IPN Parameters ===");
        params.forEach((key, value) -> System.out.println(key + " = " + value));
        
        // Validate signature for IPN
        boolean isValidSignature = MoMoSignatureUtils.isValidSignature(params, config.getSecretKey(), config.getAccessKey());
        System.out.println("IPN Signature validation result: " + isValidSignature);
        
        if (!isValidSignature) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid signature");
            return;
        }
        
        // Process IPN - update order status
        String momoOrderId = params.get("orderId");
        String resultCode = params.get("resultCode");
        
        if (momoOrderId != null) {
            Long orderId;
            try {
                if (momoOrderId.contains("-")) {
                    orderId = Long.parseLong(momoOrderId.split("-")[0]);
                } else {
                    orderId = Long.parseLong(momoOrderId);
                }
                
                if ("0".equals(resultCode)) {
                    orderService.updateOrderStatus(orderId, OrderStatus.PAID);
                    System.out.println("Order " + orderId + " marked as PAID via IPN");
                } else {
                    orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_FAILED);
                    System.out.println("Order " + orderId + " marked as PAYMENT_FAILED via IPN");
                }
            } catch (NumberFormatException e) {
                System.err.println("Error parsing orderId in IPN: " + momoOrderId);
            }
        }
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("OK");
    }
}

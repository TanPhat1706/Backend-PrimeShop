package com.primeshop.payment.vnpay;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paypal.sdk.models.OrderStatus;
import com.primeshop.order.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payment/vnpay")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class VNPayController {
    
    private final VNPayService vnPayService;
    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody PaymentRequest request) throws UnsupportedEncodingException {
        String url = vnPayService.createPaymentUrl(request);
        return ResponseEntity.ok(Map.of("paymentUrl", url));
    }

    @GetMapping("/return")
    public ResponseEntity<?> vnpReturn(@RequestParam Map<String, String> allParams) {
        String responseCode = allParams.get("vnp_ResponseCode");
        String orderId = allParams.get("vnp_TxnRef");

        String redirectUrl;
        if ("00".equals(responseCode)) {
            orderService.updateOrderStatus(Long.parseLong(orderId), com.primeshop.order.OrderStatus.PAID);
            redirectUrl = "http://localhost:5173/payment/success?orderId=" + orderId;
        } else {
            orderService.updateOrderStatus(Long.parseLong(orderId), com.primeshop.order.OrderStatus.PAYMENT_FAILED);
            redirectUrl = "http://localhost:5173/payment/failed?orderId=" + orderId;
        }

        return ResponseEntity
            .status(HttpStatus.FOUND) // 302 redirect
            .location(URI.create(redirectUrl))
            .build();
    }
}

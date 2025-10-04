package com.primeshop.payment.paypal;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.primeshop.order.Order;
import com.primeshop.order.OrderRepo;
import com.primeshop.order.OrderService;
import com.primeshop.order.OrderStatus;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/paypal")
@CrossOrigin(origins = "http://localhost:5173")
public class PayPalController {

    private final PayPalService payPalService;
    private final OrderRepo orderRepo;
    private final OrderService orderService;
    private static final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public PayPalController(PayPalService payPalService, OrderRepo orderRepo, OrderService orderService) {
        this.payPalService = payPalService;
        this.orderRepo = orderRepo;
        this.orderService = orderService;
    }

    @GetMapping("/token")
    public ResponseEntity<String> getToken() {
        return ResponseEntity.ok(payPalService.getAccessToken());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody PayPalRequest request) throws Exception {
        Optional<Order> orderOptional = orderRepo.findById(request.getOrderId());
        if (orderOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found");
        }

        Order order = orderOptional.get();
        if (!order.getStatus().equals(OrderStatus.CONFIRMED)) {
            throw new RuntimeException("Order not confirmed yet");
        }

        // Convert VND to USD (assuming 1 USD = 24,000 VND), round up to unit
        BigDecimal vndAmount = order.getTotalAmount();
        BigDecimal exchangeRate = new BigDecimal("24000");
        BigDecimal amount = vndAmount.divide(exchangeRate, 0, BigDecimal.ROUND_UP);
        String currency = "USD";

        PayPalResponse paypalRes = payPalService.createOrder(amount, currency);

        order.setPaypalPaymentId(paypalRes.getId());
        orderRepo.save(order);

        return ResponseEntity.ok(paypalRes);
    }


    @GetMapping("/return")
    public void handleReturn(@RequestParam("token") String orderId, HttpServletResponse response) throws IOException {
        try {
            String accessToken = payPalService.getAccessToken();

            String captureUrl = "https://api-m.sandbox.paypal.com/v2/checkout/orders/" + orderId + "/capture";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            System.out.println("Using accessToken: " + accessToken);
            System.out.println("Capture URL: " + captureUrl);

            HttpEntity<String> entity = new HttpEntity<>(null, headers);
            ResponseEntity<String> paypalResponse =
                    restTemplate.exchange(captureUrl, HttpMethod.POST, entity, String.class);

            Order order = orderRepo.findByPaypalPaymentId(orderId);
            if (paypalResponse.getStatusCode().is2xxSuccessful()) {
                orderService.updateOrderStatus(order.getId(), OrderStatus.PAID);
                response.sendRedirect("http://localhost:5173/payment-success?orderId=" + order.getId());
                return;
            } else {
                orderService.updateOrderStatus(order.getId(), OrderStatus.PAYMENT_FAILED);
                response.sendRedirect("http://localhost:5173/payment-failed?orderId=" + order.getId());
            }

        } catch (Exception e) {
            e.printStackTrace();
            Order order = orderRepo.findByPaypalPaymentId(orderId);
            response.sendRedirect("http://localhost:5173/payment-failed?orderId=" + order.getId());
        }
    }

    @GetMapping("/cancel")
    public void cancelOrder(@RequestParam(value = "token", required = false) String paypalOrderId, HttpServletResponse response) throws IOException {
        response.sendRedirect("http://localhost:5173/payment-cancelled");
    }
}

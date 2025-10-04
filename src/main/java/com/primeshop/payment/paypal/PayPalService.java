package com.primeshop.payment.paypal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.primeshop.order.Order;
import com.primeshop.order.OrderRepo;
import com.primeshop.order.OrderService;
import com.primeshop.order.OrderStatus;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;

@Service
public class PayPalService {
    
    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode;

    @Value("${paypal.base-url}")
    private String baseUrl;

    private final OrderRepo orderRepo;
    private final OrderService orderService;
    private static final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public PayPalService (OrderService orderService, OrderRepo orderRepo) {
        this.orderService = orderService;
        this.orderRepo = orderRepo;
    }

    public PaypalCaptureResponse captureOrder(String orderId) {
        String accessToken = getAccessToken();

        String url = baseUrl + "/v2/checkout/orders/" + orderId + "/capture";
        System.out.println("Call PayPalCapture URL: " + url);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<PaypalCaptureResponse> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, PaypalCaptureResponse.class);

        return response.getBody();
    }

    public String getAccessToken() {
        String url = baseUrl + "/v1/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("access_token")) {
            return (String) responseBody.get("access_token");
        } else {
            throw new RuntimeException("Failed to retrieve access token from PayPal");
        }
    }

    public PayPalResponse createOrder(BigDecimal amount, String currency) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        String accessToken = getAccessToken();
        
        headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> orderRequest = Map.of(
            "intent", "CAPTURE",
            "purchase_units", List.of(Map.of(
                "amount", Map.of(
                    "currency_code", currency,
                    "value", amount.toString()
                )
            )),
            "application_context", Map.of(
                "return_url", "https://localhost:8080/api/paypal/return",
                "cancel_urd", "https://localhost:8080/api/paypal/cancel"
            )
        );

        ResponseEntity<Map> orderRes = restTemplate.postForEntity(
            baseUrl + "/v2/checkout/orders", 
            new HttpEntity<>(orderRequest, headers), 
            Map.class
        );

        List<Map<String, Object>> links = (List<Map<String, Object>>) orderRes.getBody().get("links");
        String approveUrl = links.stream()
                .filter(link -> "approve".equals(link.get("rel")))
                .findFirst()
                .map(link -> (String) link.get("href"))
                .orElseThrow();

        PayPalResponse payPalResponse = new PayPalResponse();
        payPalResponse.setStatus((String) orderRes.getBody().get("status"));
        payPalResponse.setLinks(new String[]{approveUrl});
        Object idObj = orderRes.getBody().get("id");
        if (idObj != null) {
            payPalResponse.setId(idObj.toString());
        }
        return payPalResponse;
    }

    public boolean capturePayPalOrder(String paypalOrderId) {
        String url = "https://api-m.sandbox.paypal.com/v2/checkout/orders/" + paypalOrderId + "/capture";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody().contains("\"status\":\"COMPLETED\"")) {
                Order order = orderRepo.findByPaypalPaymentId(paypalOrderId);
                orderService.updateOrderStatus(order.getId(), OrderStatus.PAID);
                orderRepo.save(order);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Order order = orderRepo.findByPaypalPaymentId(paypalOrderId);
        if (order != null) {
            orderService.updateOrderStatus(order.getId(), OrderStatus.CANCELLED);
            orderRepo.save(order);
        }

        return false;
    }

    public Long findOrderIdByPaypalOrderId(String paypalOrderId) {
        return orderRepo.findByPaypalPaymentId(paypalOrderId).getId();
    }

    public String executePayment(String paymentId, String payerId) {
        APIContext apiContext = new APIContext(clientId, clientSecret, mode);
        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);

        try {
            Payment executedPayment = payment.execute(apiContext, paymentExecution);
            return executedPayment.getState();
        } catch (PayPalRESTException e) {
            System.err.println("PayPal Error: " + e.getDetails().toJSON());
        }
        return null;
    }
}

package com.primeshop.payment;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {
    @Autowired
    private VNPayService vnPayService;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody PaymentRequest request) throws UnsupportedEncodingException {
        return ResponseEntity.ok(vnPayService.createPaymentUrl(request));
    }

    @GetMapping("/callback")
    public ResponseEntity<PaymentCallbackResult> paymentCallback(@RequestParam Map<String, String> params) {
        PaymentCallbackResult result = vnPayService.handleCallback(params);
        return ResponseEntity.ok(result);

        // String redirectUrl = UriComponentsBuilder
        //     .fromUriString("https://primeshop-vnpay.loca.lt/payment-result")
        //     .queryParam("status", params.get("vnp_ResponseCode")) // thường là "00" nếu thành công
        //     .queryParam("orderId", params.get("vnp_TxnRef"))
        //     .build()
        //     .toUriString();

        // return new RedirectView(redirectUrl);
    }
}

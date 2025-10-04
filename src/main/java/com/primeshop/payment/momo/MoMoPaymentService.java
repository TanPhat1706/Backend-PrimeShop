package com.primeshop.payment.momo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.primeshop.config.MoMoConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoMoPaymentService {
    
    private final MoMoConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MoMoPaymentResponse createPayment(String orderId, Long amount, String orderInfo) throws Exception {
        String requestId = UUID.randomUUID().toString();
        String rawSignature = "accessKey=" + config.getAccessKey() +
                "&amount=" + amount +
                "&extraData=" +
                "&ipnUrl=" + config.getNotifyUrl() +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + config.getPartnerCode() +
                "&redirectUrl=" + config.getReturnUrl() +
                "&requestId=" + requestId +
                "&requestType=payWithATM";

        String signature = hmacSHA256(rawSignature, config.getSecretKey());

        MoMoPaymentRequest request = MoMoPaymentRequest.builder()
                .partnerCode(config.getPartnerCode())
                .partnerName("PrimeShop")
                .storeId("PrimeShopStore")
                .requestId(requestId)
                .amount(amount.toString())
                .orderId(orderId)
                .orderInfo(orderInfo)
                .redirectUrl(config.getReturnUrl())
                .ipnUrl(config.getNotifyUrl())
                .requestType("payWithATM")
                .signature(signature)
                .lang("vi")
                .autoCapture(true)
                .extraData("")
                .build();

        String jsonPayload = objectMapper.writeValueAsString(request);
        System.out.println("=== MoMo Request JSON ===");
        System.out.println(jsonPayload);

        URL url = new URL(config.getEndpoint());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestMethod("POST");

        try (OutputStream os = conn.getOutputStream()) { 
            os.write(objectMapper.writeValueAsBytes(request));
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            MoMoPaymentResponse response = objectMapper.readValue(br, MoMoPaymentResponse.class);
            return response;
        }


    }

    private String hmacSHA256(String data, String secret) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKeySpec);
        byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(2 * hash.length);
        for (byte b : hash) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}

package com.primeshop.payment;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.primeshop.order.OrderResponse;
import com.primeshop.order.OrderService;
import com.primeshop.order.OrderStatus;
import com.tencent.polaris.logging.LoggerFactory;

@Service
public class VNPayService {
    private static final String VNP_TMN_CODE = "P8WZY8S4"; // Thay bằng TMN_CODE từ email VNPay
    private static final String VNP_HASH_SECRET = "AYAZFPB4UZMSBE23W110JC1WPVMG0DOV"; // Thay bằng HASH_SECRET từ email VNPay
    private static final String VNP_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String VNP_RETURN_URL = "http://localhost:5173/payment-result"; // Thay bằng URL từ ngrok
    private static final String VNP_IPN_URL = "http://localhost:8080/api/payment/callback";

    private final OrderService orderService;
    private final Logger logger = LoggerFactory.getLogger(VNPayService.class);

    @Autowired
    public VNPayService(OrderService orderService) {
        this.orderService = orderService;
    }

    public String createPaymentUrl(PaymentRequest request) throws UnsupportedEncodingException {
        Long orderId = request.getOrderId();
        BigDecimal amount = request.getAmount();

        // Kiểm tra tham số đầu vào
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be a positive number");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be a positive value");
        }

        String vnpVersion = "2.1.0";
        String vnpCommand = "pay";
        String vnpOrderInfo = "Thanh toan don hang #" + orderId;
        String vnpOrderType = "billpayment";
        String vnpTxnRef = String.valueOf(orderId);
        String vnpIpAddr = "127.0.0.1";
        String vnpLocale = "vn";
        String vnpCurrCode = "VND";

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", VNP_TMN_CODE);
        vnpParams.put("vnp_Amount", String.valueOf(amount.multiply(new BigDecimal(100)).longValue()));
        vnpParams.put("vnp_CurrCode", vnpCurrCode);
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", vnpOrderInfo);
        vnpParams.put("vnp_OrderType", vnpOrderType);
        vnpParams.put("vnp_Locale", vnpLocale);
        vnpParams.put("vnp_ReturnUrl", VNP_RETURN_URL);
        vnpParams.put("vnp_IpAddr", vnpIpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString())).append('&');
                query.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString())).append('&');
            }
        }
        hashData.setLength(hashData.length() - 1);
        query.setLength(query.length() - 1);

        String vnpSecureHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        return VNP_URL + "?" + query.toString();
    }

    public String hmacSHA512(final String key, final String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC SHA512", e);
        }
    }

    public PaymentCallbackResult handleCallback(Map<String, String> params) {
        logger.info("Received VNPay callback with params: {}", params);
        System.out.println("Received VNPay callback with params: " + params);

        String vnpResponseCode = params.get("vnp_ResponseCode");
        String vnpTxnRef = params.get("vnp_TxnRef");

        if (vnpResponseCode == null || vnpTxnRef == null) {
            logger.warn("Missing required VNPay parameters.");
            return PaymentCallbackResult.invalid("Thiếu tham số callback");
        }

        try {
            Long orderId = Long.parseLong(vnpTxnRef);
            OrderResponse order = orderService.getOrderById(orderId);
            if (order == null) {
                logger.warn("Không tìm thấy đơn hàng với ID: {}", orderId);
                return PaymentCallbackResult.invalid("Không tìm thấy đơn hàng");
            }

            if ("00".equals(vnpResponseCode)) {
                orderService.updateOrderStatus(orderId, OrderStatus.PAID);
                logger.info("Thanh toán thành công cho đơn hàng {}", orderId);
                return PaymentCallbackResult.success("Thanh toán thành công");
            } else {
                orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_FAILED);
                logger.info("Thanh toán thất bại cho đơn hàng {}", orderId);
                return PaymentCallbackResult.failed("Thanh toán thất bại");
            }
        } catch (NumberFormatException ex) {
            logger.error("Sai định dạng vnp_TxnRef: {}", vnpTxnRef, ex);
            return PaymentCallbackResult.invalid("Mã đơn hàng không hợp lệ");
        }
    }
}

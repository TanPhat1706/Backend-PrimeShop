package com.primeshop.payment.method.vnpay;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.primeshop.order.OrderRepo;
import com.primeshop.order.OrderStatus;
import com.primeshop.payment.transaction.PaymentTransaction;
import com.primeshop.payment.transaction.PaymentTransactionRepo;
import com.primeshop.utils.SecurityUtils;
import com.primeshop.utils.VNPayUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VNPayService {
    private final VNPayUtil vnPayUtil;
    private final PaymentTransactionRepo paymentTransactionRepo;
    private final Environment env;
    private final OrderRepo orderRepo;
    private final SecurityUtils securityUtils;

    public String createPaymentUrl(PaymentRequest request) throws UnsupportedEncodingException {
        String vnp_TmnCode = env.getProperty("vnpay.tmn-code");
        String vnp_Url = env.getProperty("vnpay.pay-url");
        String vnp_ReturnUrl = env.getProperty("vnpay.return-url");
        String secretKey = env.getProperty("vnpay.secret-key");

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnp_TmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(request.getAmount().multiply(new BigDecimal(100)).longValue()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", String.valueOf(request.getOrderId()));
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang #" + request.getOrderId());
        vnpParams.put("vnp_OrderType", "billpayment");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        vnpParams.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        String query = vnPayUtil.buildQuery(vnpParams);
        String hashData = vnPayUtil.hmacSHA512(secretKey, query);

        String paymentUrl = vnp_Url + "?" + query + "&vnp_SecureHash=" + hashData;

        Long userId = securityUtils.getCurrentUserId();

        PaymentTransaction tx = new PaymentTransaction();
        tx.setUserId(userId);
        tx.setOrderId(String.valueOf(request.getOrderId()));
        tx.setAmount(request.getAmount());
        tx.setPaymentMethod("VNPAY");
        tx.setStatus("INITIATED");
        paymentTransactionRepo.save(tx);

        return paymentUrl;
    }
    
    public PaymentCallbackResult handleReturn(Map<String, String> vnpParams) {
        String receivedHash = vnpParams.get("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        String query = vnPayUtil.buildQuery(vnpParams);
        String secretKey = env.getProperty("vnpay.secret-key");
        String expectedHash = vnPayUtil.hmacSHA512(secretKey, query);

        boolean valid = expectedHash.equals(receivedHash);

        String orderId = vnpParams.get("vnp_TxnRef");
        PaymentTransaction tx = paymentTransactionRepo.findByOrderId(orderId).orElse(null);

        if (tx == null) {
            return PaymentCallbackResult.invalid("Không tìm thấy giao dịch thanh toán");
        }

        if (!valid) {
            tx.setStatus("FAILED");
            tx.setResponseCode("INVALID_HASH");
            paymentTransactionRepo.save(tx);

            orderRepo.findById(Long.parseLong(orderId)).ifPresent(order -> {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                orderRepo.save(order);
            });

            return PaymentCallbackResult.invalid("Chữ ký không hợp lệ");
        }

        String responseCode = vnpParams.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            tx.setStatus("SUCCESS");
            tx.setTransactionCode(vnpParams.get("vnp_TransactionNo"));
            tx.setResponseCode("00");

            orderRepo.findById(Long.parseLong(orderId)).ifPresent(order -> {
                order.setStatus(OrderStatus.PAID);
                orderRepo.save(order);
            });

            paymentTransactionRepo.save(tx);
            return PaymentCallbackResult.success("Thanh toán thành công");
        } else {
            tx.setStatus("FAILED");
            tx.setResponseCode(responseCode);

            orderRepo.findById(Long.parseLong(orderId)).ifPresent(order -> {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                orderRepo.save(order);
            });

            paymentTransactionRepo.save(tx);
            return PaymentCallbackResult.failed("Thanh toán thất bại");
        }
    }
}

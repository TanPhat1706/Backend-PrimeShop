package com.primeshop.payment.method.vnpay;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.primeshop.order.OrderRepo;
import com.primeshop.order.OrderStatus;
import com.primeshop.payment.transaction.PaymentTransaction;
import com.primeshop.payment.transaction.PaymentTransactionRepo;
import com.primeshop.utils.SecurityUtils;
import com.primeshop.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VNPayService {
    private final VNPayUtil vnPayUtil;
    private final PaymentTransactionRepo paymentTransactionRepo;
    private final Environment env;
    private final OrderRepo orderRepo;
    private final SecurityUtils securityUtils;

    public String createPaymentUrl(PaymentRequest request, HttpServletRequest httpServletRequest) throws UnsupportedEncodingException {
        String vnp_TmnCode = env.getProperty("vnpay.tmn-code");
        String vnp_Url = env.getProperty("vnpay.pay-url");
        String vnp_ReturnUrl = env.getProperty("vnpay.return-url");
        String secretKey = env.getProperty("vnpay.secret-key");

        Map<String, String> vnpParams = new TreeMap<>();
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
        
        vnpParams.put("vnp_IpAddr", vnPayUtil.getIpAddress(httpServletRequest)); 
        
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(vietnamZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", now.format(formatter));
        vnpParams.put("vnp_ExpireDate", now.plusMinutes(15).format(formatter));

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
    
    @Transactional 
    public PaymentCallbackResult handleReturn(Map<String, String> vnpParams) {
        Map<String, String> fields = new HashMap<>(vnpParams);

        String receivedHash = fields.get("vnp_SecureHash");
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String query = vnPayUtil.buildQuery(fields);
        String secretKey = env.getProperty("vnpay.secret-key");
        String expectedHash = vnPayUtil.hmacSHA512(secretKey, query);

        boolean valid = expectedHash.equals(receivedHash);
        
        String orderId = fields.get("vnp_TxnRef");
        PaymentTransaction tx = paymentTransactionRepo.findFirstByOrderIdOrderByCreatedAtDesc(orderId).orElse(null);

        if (tx == null) {
            return PaymentCallbackResult.invalid("Không tìm thấy giao dịch thanh toán");
        }

        if (!valid) {
            tx.setStatus("FAILED");
            tx.setResponseCode("INVALID_HASH");
            paymentTransactionRepo.save(tx);
            
            updateOrderStatus(orderId, OrderStatus.PAYMENT_FAILED);

            return PaymentCallbackResult.invalid("Chữ ký không hợp lệ");
        }

        String responseCode = fields.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            tx.setStatus("SUCCESS");
            tx.setTransactionCode(fields.get("vnp_TransactionNo"));
            tx.setResponseCode("00");
            paymentTransactionRepo.save(tx);
            
            updateOrderStatus(orderId, OrderStatus.PAID);
            
            return PaymentCallbackResult.success("Thanh toán thành công");
        } else {
            tx.setStatus("FAILED");
            tx.setResponseCode(responseCode);
            paymentTransactionRepo.save(tx);
            
            updateOrderStatus(orderId, OrderStatus.PAYMENT_FAILED);
            
            return PaymentCallbackResult.failed("Thanh toán thất bại");
        }
    }

    private void updateOrderStatus(String orderId, OrderStatus status) {
        orderRepo.findById(Long.parseLong(orderId)).ifPresent(order -> {
            order.setStatus(status);
            orderRepo.save(order);
        });
    }
}
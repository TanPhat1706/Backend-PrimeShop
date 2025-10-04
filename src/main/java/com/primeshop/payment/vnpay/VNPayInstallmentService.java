package com.primeshop.payment.vnpay;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primeshop.installment.InstallmentAgreement;
import com.primeshop.installment.InstallmentAgreementRepo;
import com.primeshop.order.Order;
import com.primeshop.order.OrderRepo;
import com.primeshop.order.OrderStatus;
import com.primeshop.payment.PaymentTransaction;
import com.primeshop.payment.PaymentTransactionRepo;
import com.primeshop.utils.PaymentCalcUtil;
import com.primeshop.utils.VNPayUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VNPayInstallmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(VNPayInstallmentService.class);
    
    private final VNPayUtil vnPayUtil;
    private final VNPayInstallmentConfig installmentConfig;
    private final PaymentTransactionRepo paymentTransactionRepo;
    private final InstallmentAgreementRepo installmentAgreementRepo;
    private final OrderRepo orderRepo;
    private final Environment env;
    
    /**
     * Tạo URL thanh toán trả góp VNPay
     */
    @Transactional
    public VNPayInstallmentResponse createInstallmentPayment(VNPayInstallmentRequest request) {
        logger.info("Creating VNPay installment payment for order: {}", request.getOrderId());
        
        // Validate request
        validateInstallmentRequest(request);
        
        // Lấy thông tin đơn hàng
        Order order = orderRepo.findById(request.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));
        
        // Kiểm tra trạng thái đơn hàng
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order must be confirmed before creating installment payment");
        }
        
        // Tính toán lãi suất và số tiền trả góp
        BigDecimal interestRate = installmentConfig.getInterestRate(request.getInstallmentMonths());
        BigDecimal monthlyPayment = PaymentCalcUtil.calculateMonthly(
            request.getAmount(), 
            interestRate, 
            request.getInstallmentMonths()
        );
        
        // Tạo InstallmentAgreement
        InstallmentAgreement agreement = createInstallmentAgreement(order, request, interestRate, monthlyPayment);
        
        // Tạo PaymentTransaction
        PaymentTransaction transaction = createPaymentTransaction(order, request, agreement);
        
        // Tạo URL thanh toán VNPay
        String paymentUrl = createVNPayPaymentUrl(request, transaction, agreement);
        
        // Cập nhật trạng thái đơn hàng
        order.setStatus(OrderStatus.PROCESSING);
        orderRepo.save(order);
        
        logger.info("VNPay installment payment created successfully. Transaction ID: {}", transaction.getId());
        
        return VNPayInstallmentResponse.builder()
            .paymentUrl(paymentUrl)
            .transactionId(transaction.getId().toString())
            .orderId(order.getId().toString())
            .totalAmount(request.getAmount())
            .monthlyPayment(monthlyPayment)
            .installmentMonths(request.getInstallmentMonths())
            .interestRate(interestRate)
            .status("PENDING")
            .createdAt(transaction.getCreatedAt())
            .expiresAt(transaction.getCreatedAt().plusMinutes(installmentConfig.getPaymentTimeoutMinutes()))
            .message("Payment URL created successfully")
            .build();
    }
    
    /**
     * Xử lý callback từ VNPay cho trả góp
     */
    @Transactional
    public boolean handleInstallmentCallback(Map<String, String> vnpParams) {
        logger.info("Handling VNPay installment callback: {}", vnpParams);
        
        String orderId = vnpParams.get("vnp_TxnRef");
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionNo = vnpParams.get("vnp_TransactionNo");
        
        if (orderId == null || responseCode == null) {
            logger.error("Missing required parameters in VNPay callback");
            return false;
        }
        
        try {
            // Tìm PaymentTransaction
            Optional<PaymentTransaction> transactionOpt = paymentTransactionRepo.findByOrderId(orderId);
            if (transactionOpt.isEmpty()) {
                logger.error("Payment transaction not found for order: {}", orderId);
                return false;
            }
            
            PaymentTransaction transaction = transactionOpt.get();
            
            // Verify hash
            if (!verifyVNPayHash(vnpParams)) {
                logger.error("Invalid VNPay hash for order: {}", orderId);
                transaction.setStatus("FAILED");
                transaction.setResponseCode("INVALID_HASH");
                paymentTransactionRepo.save(transaction);
                return false;
            }
            
            // Xử lý kết quả thanh toán
            if ("00".equals(responseCode)) {
                handleSuccessfulPayment(transaction, transactionNo);
                logger.info("VNPay installment payment successful for order: {}", orderId);
                return true;
            } else {
                handleFailedPayment(transaction, responseCode);
                logger.warn("VNPay installment payment failed for order: {}, code: {}", orderId, responseCode);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error handling VNPay installment callback for order: {}", orderId, e);
            return false;
        }
    }
    
    /**
     * Lấy thông tin trả góp theo order ID
     */
    public Optional<InstallmentAgreement> getInstallmentAgreementByOrderId(Long orderId) {
        return installmentAgreementRepo.findByOrderId(orderId);
    }
    
    /**
     * Validate request trả góp
     */
    private void validateInstallmentRequest(VNPayInstallmentRequest request) {
        if (!installmentConfig.isValidInstallmentMonths(request.getInstallmentMonths())) {
            throw new IllegalArgumentException(
                String.format("Invalid installment months: %d. Must be between %d and %d",
                    request.getInstallmentMonths(),
                    installmentConfig.getMinInstallmentMonths(),
                    installmentConfig.getMaxInstallmentMonths())
            );
        }
        
        if (!installmentConfig.isValidAmount(request.getAmount())) {
            throw new IllegalArgumentException(
                String.format("Invalid amount: %s. Must be between %s and %s VND",
                    request.getAmount(),
                    installmentConfig.getMinAmount(),
                    installmentConfig.getMaxAmount())
            );
        }
    }
    
    /**
     * Tạo InstallmentAgreement
     */
    private InstallmentAgreement createInstallmentAgreement(Order order, VNPayInstallmentRequest request, 
                                                           BigDecimal interestRate, BigDecimal monthlyPayment) {
        InstallmentAgreement agreement = new InstallmentAgreement();
        agreement.setOrderId(order.getId());
        agreement.setUserId(order.getUser().getId());
        agreement.setAmount(request.getAmount().multiply(new BigDecimal(100)).longValue()); // Convert to cents
        agreement.setMonths(request.getInstallmentMonths());
        agreement.setAnnualRate(interestRate);
        agreement.setStatus("PENDING");
        agreement.setReferenceCode(UUID.randomUUID().toString());
        agreement.setCreatedAt(java.time.Instant.now());
        agreement.setUpdatedAt(java.time.Instant.now());
        
        return installmentAgreementRepo.save(agreement);
    }
    
    /**
     * Tạo PaymentTransaction
     */
    private PaymentTransaction createPaymentTransaction(Order order, VNPayInstallmentRequest request, 
                                                      InstallmentAgreement agreement) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(order.getId().toString());
        transaction.setAmount(request.getAmount());
        transaction.setStatus("PENDING");
        transaction.setPaymentMethod("VNPAY_INSTALLMENT");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        
        return paymentTransactionRepo.save(transaction);
    }
    
    /**
     * Tạo URL thanh toán VNPay
     */
    private String createVNPayPaymentUrl(VNPayInstallmentRequest request, PaymentTransaction transaction, 
                                       InstallmentAgreement agreement) {
        String vnp_TmnCode = env.getProperty("vnpay.tmn-code");
        String vnp_Url = env.getProperty("vnpay.pay-url");
        String vnp_ReturnUrl = env.getProperty("vnpay.return-url");
        String secretKey = env.getProperty("vnpay.secret-key");
        
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnp_TmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(request.getAmount().multiply(new BigDecimal(100)).longValue()));
        vnpParams.put("vnp_CurrCode", installmentConfig.getCurrency());
        vnpParams.put("vnp_TxnRef", transaction.getId().toString());
        vnpParams.put("vnp_OrderInfo", String.format("Tra gop don hang #%s - %d thang", 
            request.getOrderId(), request.getInstallmentMonths()));
        vnpParams.put("vnp_OrderType", installmentConfig.getOrderType());
        vnpParams.put("vnp_Locale", installmentConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        vnpParams.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        
        // Thêm thông tin trả góp
        vnpParams.put("vnp_InstallmentMonths", request.getInstallmentMonths().toString());
        vnpParams.put("vnp_InterestRate", installmentConfig.getInterestRate(request.getInstallmentMonths()).toString());
        
        String query = vnPayUtil.buildQuery(vnpParams);
        String hashData = vnPayUtil.hmacSHA512(secretKey, query);
        
        return vnp_Url + "?" + query + "&vnp_SecureHash=" + hashData;
    }
    
    /**
     * Verify VNPay hash
     */
    private boolean verifyVNPayHash(Map<String, String> vnpParams) {
        String receivedHash = vnpParams.get("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");
        
        String query = vnPayUtil.buildQuery(vnpParams);
        String secretKey = env.getProperty("vnpay.secret-key");
        String expectedHash = vnPayUtil.hmacSHA512(secretKey, query);
        
        return expectedHash.equals(receivedHash);
    }
    
    /**
     * Xử lý thanh toán thành công
     */
    private void handleSuccessfulPayment(PaymentTransaction transaction, String transactionNo) {
        transaction.setStatus("SUCCESS");
        transaction.setTransactionNo(transactionNo);
        transaction.setResponseCode("00");
        transaction.setUpdatedAt(LocalDateTime.now());
        paymentTransactionRepo.save(transaction);
        
        // Cập nhật trạng thái đơn hàng
        Order order = orderRepo.findById(Long.parseLong(transaction.getOrderId())).orElse(null);
        if (order != null) {
            order.setStatus(OrderStatus.PAID);
            orderRepo.save(order);
        }
        
        // Cập nhật trạng thái InstallmentAgreement
        Optional<InstallmentAgreement> agreementOpt = installmentAgreementRepo.findByOrderId(Long.parseLong(transaction.getOrderId()));
        if (agreementOpt.isPresent()) {
            InstallmentAgreement agreement = agreementOpt.get();
            agreement.setStatus("ACTIVE");
            agreement.setUpdatedAt(java.time.Instant.now());
            installmentAgreementRepo.save(agreement);
        }
    }
    
    /**
     * Xử lý thanh toán thất bại
     */
    private void handleFailedPayment(PaymentTransaction transaction, String responseCode) {
        transaction.setStatus("FAILED");
        transaction.setResponseCode(responseCode);
        transaction.setUpdatedAt(LocalDateTime.now());
        paymentTransactionRepo.save(transaction);
        
        // Cập nhật trạng thái đơn hàng
        Order order = orderRepo.findById(Long.parseLong(transaction.getOrderId())).orElse(null);
        if (order != null) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepo.save(order);
        }
        
        // Cập nhật trạng thái InstallmentAgreement
        Optional<InstallmentAgreement> agreementOpt = installmentAgreementRepo.findByOrderId(Long.parseLong(transaction.getOrderId()));
        if (agreementOpt.isPresent()) {
            InstallmentAgreement agreement = agreementOpt.get();
            agreement.setStatus("FAILED");
            agreement.setUpdatedAt(java.time.Instant.now());
            installmentAgreementRepo.save(agreement);
        }
    }
}

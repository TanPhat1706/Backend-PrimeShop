// package com.primeshop.payment.vnpay;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// import java.math.BigDecimal;
// import java.util.Optional;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.core.env.Environment;

// import com.primeshop.installment.InstallmentAgreement;
// import com.primeshop.installment.InstallmentAgreementRepo;
// import com.primeshop.order.Order;
// import com.primeshop.order.OrderRepo;
// import com.primeshop.order.OrderStatus;
// import com.primeshop.payment.method.vnpayinstallment.VNPayInstallmentConfig;
// import com.primeshop.payment.method.vnpayinstallment.VNPayInstallmentRequest;
// import com.primeshop.payment.method.vnpayinstallment.VNPayInstallmentResponse;
// import com.primeshop.payment.method.vnpayinstallment.VNPayInstallmentService;
// import com.primeshop.payment.transaction.PaymentTransaction;
// import com.primeshop.payment.transaction.PaymentTransactionRepo;
// import com.primeshop.user.User;
// import com.primeshop.utils.VNPayUtil;

// @ExtendWith(MockitoExtension.class)
// class VNPayInstallmentServiceTest {

//     @Mock
//     private VNPayUtil vnPayUtil;
    
//     @Mock
//     private VNPayInstallmentConfig installmentConfig;
    
//     @Mock
//     private PaymentTransactionRepo paymentTransactionRepo;
    
//     @Mock
//     private InstallmentAgreementRepo installmentAgreementRepo;
    
//     @Mock
//     private OrderRepo orderRepo;
    
//     @Mock
//     private Environment env;
    
//     @InjectMocks
//     private VNPayInstallmentService installmentService;
    
//     private VNPayInstallmentRequest validRequest;
//     private Order validOrder;
//     private User validUser;
    
//     @BeforeEach
//     void setUp() {
//         // Setup valid request
//         validRequest = new VNPayInstallmentRequest();
//         validRequest.setOrderId(1L);
//         validRequest.setAmount(new BigDecimal("1000000"));
//         validRequest.setInstallmentMonths(6);
//         validRequest.setCustomerName("Test User");
//         validRequest.setCustomerPhone("0123456789");
//         validRequest.setCustomerEmail("test@example.com");
//         validRequest.setDescription("Test installment payment");
        
//         // Setup valid user
//         validUser = new User();
//         validUser.setId(1L);
//         // validUser.setActive(true);
        
//         // Setup valid order
//         validOrder = new Order();
//         validOrder.setId(1L);
//         validOrder.setUser(validUser);
//         validOrder.setTotalAmount(new BigDecimal("1000000"));
//         validOrder.setStatus(OrderStatus.CONFIRMED);
//         validOrder.setDeleted(false);
//     }
    
//     @Test
//     void testCreateInstallmentPayment_Success() {
//         // Given
//         when(installmentConfig.isValidInstallmentMonths(6)).thenReturn(true);
//         when(installmentConfig.isValidAmount(new BigDecimal("1000000"))).thenReturn(true);
//         when(installmentConfig.getInterestRate(6)).thenReturn(new BigDecimal("9.0"));
//         when(orderRepo.findById(1L)).thenReturn(Optional.of(validOrder));
//         when(installmentAgreementRepo.save(any(InstallmentAgreement.class))).thenAnswer(invocation -> {
//             InstallmentAgreement agreement = invocation.getArgument(0);
//             agreement.setId(1L);
//             return agreement;
//         });
//         when(paymentTransactionRepo.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
//             PaymentTransaction transaction = invocation.getArgument(0);
//             transaction.setId(1L);
//             return transaction;
//         });
//         when(orderRepo.save(any(Order.class))).thenReturn(validOrder);
//         when(env.getProperty("vnpay.tmn-code")).thenReturn("TEST_TMN");
//         when(env.getProperty("vnpay.pay-url")).thenReturn("https://test.vnpay.vn");
//         when(env.getProperty("vnpay.return-url")).thenReturn("https://test.com/return");
//         when(env.getProperty("vnpay.secret-key")).thenReturn("TEST_SECRET");
//         when(vnPayUtil.buildQuery(any())).thenReturn("test_query");
//         when(vnPayUtil.hmacSHA512(anyString(), anyString())).thenReturn("test_hash");
        
//         // When
//         VNPayInstallmentResponse response = installmentService.createInstallmentPayment(validRequest);
        
//         // Then
//         assertNotNull(response);
//         assertEquals("1", response.getOrderId());
//         assertEquals(new BigDecimal("1000000"), response.getTotalAmount());
//         assertEquals(6, response.getInstallmentMonths());
//         assertEquals(new BigDecimal("9.0"), response.getInterestRate());
//         assertEquals("PENDING", response.getStatus());
//         assertNotNull(response.getPaymentUrl());
//         assertNotNull(response.getTransactionId());
        
//         // Verify interactions
//         verify(installmentAgreementRepo).save(any(InstallmentAgreement.class));
//         verify(paymentTransactionRepo).save(any(PaymentTransaction.class));
//         verify(orderRepo).save(any(Order.class));
//     }
    
//     @Test
//     void testCreateInstallmentPayment_InvalidInstallmentMonths() {
//         // Given
//         validRequest.setInstallmentMonths(1); // Invalid: less than minimum
//         when(installmentConfig.isValidInstallmentMonths(1)).thenReturn(false);
//         when(installmentConfig.getMinInstallmentMonths()).thenReturn(3);
//         when(installmentConfig.getMaxInstallmentMonths()).thenReturn(24);
        
//         // When & Then
//         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//             installmentService.createInstallmentPayment(validRequest);
//         });
        
//         assertTrue(exception.getMessage().contains("Invalid installment months"));
//     }
    
//     @Test
//     void testCreateInstallmentPayment_InvalidAmount() {
//         // Given
//         validRequest.setAmount(new BigDecimal("500000")); // Invalid: less than minimum
//         when(installmentConfig.isValidInstallmentMonths(6)).thenReturn(true);
//         when(installmentConfig.isValidAmount(new BigDecimal("500000"))).thenReturn(false);
//         when(installmentConfig.getMinAmount()).thenReturn(new BigDecimal("1000000"));
//         when(installmentConfig.getMaxAmount()).thenReturn(new BigDecimal("50000000"));
        
//         // When & Then
//         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//             installmentService.createInstallmentPayment(validRequest);
//         });
        
//         assertTrue(exception.getMessage().contains("Invalid amount"));
//     }
    
//     @Test
//     void testCreateInstallmentPayment_OrderNotFound() {
//         // Given
//         when(installmentConfig.isValidInstallmentMonths(6)).thenReturn(true);
//         when(installmentConfig.isValidAmount(new BigDecimal("1000000"))).thenReturn(true);
//         when(orderRepo.findById(1L)).thenReturn(Optional.empty());
        
//         // When & Then
//         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//             installmentService.createInstallmentPayment(validRequest);
//         });
        
//         assertTrue(exception.getMessage().contains("Order not found"));
//     }
    
//     @Test
//     void testCreateInstallmentPayment_OrderNotConfirmed() {
//         // Given
//         validOrder.setStatus(OrderStatus.PENDING);
//         when(installmentConfig.isValidInstallmentMonths(6)).thenReturn(true);
//         when(installmentConfig.isValidAmount(new BigDecimal("1000000"))).thenReturn(true);
//         when(orderRepo.findById(1L)).thenReturn(Optional.of(validOrder));
        
//         // When & Then
//         IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
//             installmentService.createInstallmentPayment(validRequest);
//         });
        
//         assertTrue(exception.getMessage().contains("Order must be confirmed"));
//     }
    
//     @Test
//     void testGetInstallmentAgreementByOrderId_Success() {
//         // Given
//         InstallmentAgreement agreement = new InstallmentAgreement();
//         agreement.setId(1L);
//         agreement.setOrderId(1L);
//         when(installmentAgreementRepo.findByOrderId(1L)).thenReturn(Optional.of(agreement));
        
//         // When
//         Optional<InstallmentAgreement> result = installmentService.getInstallmentAgreementByOrderId(1L);
        
//         // Then
//         assertTrue(result.isPresent());
//         assertEquals(1L, result.get().getId());
//         assertEquals(1L, result.get().getOrderId());
//     }
    
//     @Test
//     void testGetInstallmentAgreementByOrderId_NotFound() {
//         // Given
//         when(installmentAgreementRepo.findByOrderId(1L)).thenReturn(Optional.empty());
        
//         // When
//         Optional<InstallmentAgreement> result = installmentService.getInstallmentAgreementByOrderId(1L);
        
//         // Then
//         assertFalse(result.isPresent());
//     }
// }

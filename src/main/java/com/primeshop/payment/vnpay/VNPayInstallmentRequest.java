package com.primeshop.payment.vnpay;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VNPayInstallmentRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotNull(message = "Installment months is required")
    @Min(value = 1, message = "Installment months must be at least 1")
    private Integer installmentMonths;
    
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String description;
}

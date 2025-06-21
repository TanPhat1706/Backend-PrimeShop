package com.primeshop.payment;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentRequest {
    private Long orderId;
    private BigDecimal amount;
}

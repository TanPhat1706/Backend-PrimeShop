package com.primeshop.payment.paypal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PayPalRequest {
    private Long orderId;
}

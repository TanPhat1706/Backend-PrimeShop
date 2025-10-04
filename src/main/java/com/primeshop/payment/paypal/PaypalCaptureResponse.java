package com.primeshop.payment.paypal;

import lombok.Data;

@Data
public class PaypalCaptureResponse {
    private String id;
    private String status;
}

package com.primeshop.payment.momo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MoMoIpnResponse {
    private String status;
    private String message;
}

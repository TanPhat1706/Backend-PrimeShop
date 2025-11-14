package com.primeshop.payment;

import org.springframework.stereotype.Component;

@Component
public class VNPayConfig {
    public static final String VNP_VERSION = "2.1.0";
    public static final String VNP_COMMAND = "pay";
    public static final String VNP_TMN_CODE = "P8WZY8S4"; // TODO: Đặt trong .properties
    public static final String VNP_HASH_SECRET = "AYAZFPB4UZMSBE23W110JC1WPVMG0DOV";
    public static final String VNP_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String VNP_RETURN_URL = "https://primeshop-vnpay.loca.lt/api/payment/callback";
    public static final String VNP_LOCALE = "vn";
    public static final String VNP_CURR_CODE = "VND";
}

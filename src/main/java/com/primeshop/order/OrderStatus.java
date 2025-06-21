package com.primeshop.order;

public enum OrderStatus {
    PENDING,       // Đang chờ xác nhận
    CONFIRMED,     // Đã xác nhận, sẵn sàng thanh toán
    PAID,          // Thanh toán thành công
    PROCESSING,    // Đang xử lý
    SHIPPED,       // Đã giao hàng
    DELIVERED,     // Đã nhận hàng
    PAYMENT_FAILED, // Thanh toán thất bại
    CANCELLED      // Đã hủy
}

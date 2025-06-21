package com.primeshop.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderFilterRequest {
    private Long userId;
    private Long orderId;
    private OrderStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isDeleted;
    private BigDecimal minTotalAmount;
    private BigDecimal maxTotalAmount;
}

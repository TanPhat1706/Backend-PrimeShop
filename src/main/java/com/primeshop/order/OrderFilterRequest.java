package com.primeshop.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderFilterRequest {
    private Long userId;
    private Long orderId;
    private List<OrderStatus> status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isDeleted;
    private BigDecimal minTotalAmount;
    private BigDecimal maxTotalAmount;

    public List<OrderStatus> getStatus() { return status; }
    public void setStatus(List<OrderStatus> status) { this.status = status; }
}

package com.primeshop.bnpl;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BNPLInstallmentDTO {
    private Long id;
    private int installmentNumber;
    private BigDecimal amount;
    private LocalDateTime dueDate;
    private boolean paid;
}

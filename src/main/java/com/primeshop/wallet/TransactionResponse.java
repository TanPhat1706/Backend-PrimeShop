package com.primeshop.wallet;

import java.util.Date;

public class TransactionResponse {
    private Long id;
    private Double amount;
    private String type;
    private String description;
    private Date createdAt;

    public TransactionResponse(Long id, Double amount, String type, String description, Date createdAt) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }
    // Getters & Setters
    // ...
}
package com.primeshop.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepo extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByOrderId(String orderId);
}

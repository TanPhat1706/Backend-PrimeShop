package com.primeshop.chatc2c;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findBySellerIdAndCustomerId(Long sellerId, Long customerId);
    List<Conversation> findByCustomerId(Long customerId);
    List<Conversation> findBySellerIdOrCustomerId(Long sellerId, Long customerId);
}

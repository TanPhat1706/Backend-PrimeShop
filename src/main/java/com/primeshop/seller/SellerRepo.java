package com.primeshop.seller;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.primeshop.seller.SellerProfile.SellerStatus;

public interface SellerRepo extends JpaRepository<SellerProfile, Long> {
    Optional<SellerProfile> findByUserId(Long userId);
    List<SellerProfile> findByStatus(SellerStatus status);
}

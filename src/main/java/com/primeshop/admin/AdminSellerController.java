package com.primeshop.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.primeshop.product.Product;
import com.primeshop.product.ProductResponse;
import com.primeshop.seller.SellerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/seller")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSellerController {
    private final SellerService sellerService;

    @PatchMapping("/approve-registration")
    public String approveSeller(@RequestParam Long sellerId) {
        return "Đã phê duyệt người bán thành công: " + sellerService.approveSeller(sellerId).getShopName();
    }

    @PatchMapping("/approve-products")
    public ProductResponse approveSellerProducts(@RequestParam Long sellerId, @RequestParam Long productId) {
        return sellerService.approveSellerProducts(sellerId, productId);
    }
}

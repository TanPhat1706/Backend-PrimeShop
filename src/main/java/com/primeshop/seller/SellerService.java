package com.primeshop.seller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.primeshop.order.OrderResponse;
import com.primeshop.product.Product;
import com.primeshop.product.ProductRepo;
import com.primeshop.product.ProductResponse;
import com.primeshop.product.Product.ProductStatus;
import com.primeshop.seller.SellerProfile.SellerStatus;
import com.primeshop.user.Role;
import com.primeshop.user.RoleRepo;
import com.primeshop.user.User;
import com.primeshop.user.UserRepo;

@Service
public class SellerService {
    @Autowired
    private final UserRepo userRepo;
    @Autowired
    private final SellerRepo sellerRepo;
    @Autowired
    private final ProductRepo productRepo;
    @Autowired
    private final RoleRepo roleRepo;

    public SellerService(UserRepo userRepo, SellerRepo sellerRepo, ProductRepo productRepo, RoleRepo roleRepo) {
        this.userRepo = userRepo;
        this.sellerRepo = sellerRepo;
        this.productRepo = productRepo;
        this.roleRepo = roleRepo;
    }

    public SellerResponse registerSeller(SellerRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        if (sellerRepo.findByUserId(user.getId()) != null) {
            throw new IllegalArgumentException("Seller profile already exists for this user");
        }

        Role sellerRole = roleRepo.findByName(Role.RoleName.ROLE_SELLER)
            .orElseThrow(() -> new RuntimeException("Role không tồn tại trong hệ thống"));


        SellerProfile sellerProfile = new SellerProfile();
        sellerProfile.setUser(user);
        sellerProfile.setShopName(request.getShopName());
        sellerProfile.setIdentityCard(request.getIdentityCard());
        sellerProfile.setDescription(request.getDescription());
        sellerProfile.setPhone(request.getPhone());
        sellerProfile.setStatus(SellerStatus.PENDING_REVIEW);
        sellerRepo.save(sellerProfile);
        user.getRoles().add(sellerRole);
        userRepo.save(user);
        return new SellerResponse(sellerProfile);
    }

    public SellerProfile approveSeller(Long sellerId) {
        SellerProfile sellerProfile = sellerRepo.findById(sellerId)
            .orElseThrow(() -> new IllegalArgumentException("Seller profile not found"));
        if (sellerProfile.getStatus() != SellerStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Seller profile is not in pending review status");
        }
        sellerProfile.setStatus(SellerStatus.VERIFIED_SELLER);
        return sellerRepo.save(sellerProfile);
    }

    public ProductResponse approveSellerProducts(Long sellerId, Long productId) {
        SellerProfile sellerProfile = sellerRepo.findById(sellerId)
            .orElseThrow(() -> new IllegalArgumentException("Seller profile not found"));
        if (sellerProfile.getStatus() != SellerStatus.VERIFIED_SELLER) {
            throw new IllegalArgumentException("Seller profile is not verified");
        }
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        System.out.println("============================" + product.getStatus());
        if (product.getStatus() != ProductStatus.PENDING) {
            throw new IllegalArgumentException("Product is not in pending status");
        }
        product.setStatus(ProductStatus.APPROVED);
        productRepo.save(product);
        return new ProductResponse(product);
    }
}

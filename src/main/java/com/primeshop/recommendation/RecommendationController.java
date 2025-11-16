package com.primeshop.recommendation;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.primeshop.product.Product;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    // 🧩 Gợi ý theo lịch sử mua hàng của user
    @GetMapping("/user/{userId}")
    public List<Product> getUserRecommendations(@PathVariable Long userId) {
        return recommendationService.getRecommendations(userId);
    }

    // 🧠 (Tùy chọn) Gợi ý theo sản phẩm — nếu bạn muốn thêm sau
    // Ví dụ: Khi user đang xem 1 sản phẩm, hiển thị “Frequently Bought Together”
    @GetMapping("/product/{productId}")
    public List<Product> getProductRecommendations(@PathVariable Long productId) {
        return recommendationService.getRecommendationsByProduct(productId);
    }
}

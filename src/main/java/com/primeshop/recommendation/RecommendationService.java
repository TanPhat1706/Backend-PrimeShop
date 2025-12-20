package com.primeshop.recommendation;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

import com.primeshop.order.OrderItemRepo;
import com.primeshop.product.Product;
import com.primeshop.product.ProductRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final OrderItemRepo orderItemRepository;
    private final ProductRepo productRepository;

    public List<Product> getRecommendations(Long userId) {
        // 1️⃣ Lấy danh sách sản phẩm user đã từng mua
        List<Product> purchased = orderItemRepository.findProductsByUserId(userId);

        if (purchased.isEmpty()) {
            // Nếu user chưa từng mua gì → gợi ý sản phẩm bán chạy
            return productRepository.findAll().stream()
                    .sorted((a, b) -> b.getSold().compareTo(a.getSold()))
                    .limit(10)
                    .collect(Collectors.toList());
        }

        // 2️⃣ Lấy danh mục của sản phẩm đã mua
        Set<Long> categoryIds = purchased.stream()
                .map(p -> p.getCategory().getId())
                .collect(Collectors.toSet());

        // 3️⃣ Gợi ý sản phẩm cùng danh mục, chưa mua
        List<Product> recommended = productRepository.findRecommendedProducts(
                categoryIds, userId, PageRequest.of(0, 10));

        // 4️⃣ Nếu còn chỗ → thêm sản phẩm thường được mua cùng với sản phẩm user đã mua
        if (recommended.size() < 10) {
            Set<Long> relatedIds = new HashSet<>();
            for (Product p : purchased) {
                List<Long> related = productRepository.findFrequentlyBoughtTogether(p.getId());
                relatedIds.addAll(related);
            }

            // Chuyển sang danh sách Product
            List<Product> relatedProducts = productRepository.findAllById(relatedIds);
            relatedProducts.removeAll(purchased); // loại bỏ sản phẩm user đã mua
            recommended.addAll(relatedProducts);
        }

        // 5️⃣ Nếu vẫn chưa đủ → thêm sản phẩm bán chạy
        if (recommended.size() < 10) {
            List<Product> topSelling = productRepository.findAll().stream()
                    .sorted((a, b) -> b.getSold().compareTo(a.getSold()))
                    .limit(10 - recommended.size())
                    .collect(Collectors.toList());
            recommended.addAll(topSelling);
        }

        // 6️⃣ Xóa trùng và giới hạn 10
        return recommended.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }

    public List<Product> getRecommendationsByProduct(Long productId) {
        // 1️⃣ Lấy danh sách các productId thường được mua cùng sản phẩm này
        List<Long> relatedProductIds = productRepository.findFrequentlyBoughtTogether(productId);

        // 2️⃣ Lấy danh sách sản phẩm thực tế từ ID
        List<Product> recommended = productRepository.findAllById(relatedProductIds);

        // 3️⃣ Nếu danh sách rỗng → fallback sang top sản phẩm bán chạy
        if (recommended.isEmpty()) {
            recommended = productRepository.findByActiveTrueOrderBySoldDesc()
                    .stream()
                    .limit(10)
                    .collect(Collectors.toList());
        }

        return recommended;
    }
}

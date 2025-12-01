package com.primeshop.gemini;

import com.primeshop.product.Product;
import com.primeshop.product.ProductRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GeminiAdvisor {

    private final GeminiService geminiService;
    private final ProductRepo productRepo;

    public GeminiAdvisor(GeminiService geminiService, ProductRepo productRepo) {
        this.geminiService = geminiService;
        this.productRepo = productRepo;
    }

    public String askGemini(String question) {
        long start = System.currentTimeMillis();

        String lowerQ = question.toLowerCase();

        List<Product> filteredProducts = productRepo.findAll().stream()
                .filter(p -> {
                    String name = Optional.ofNullable(p.getName()).orElse("").toLowerCase();
                    String brand = Optional.ofNullable(p.getBrand()).orElse("").toLowerCase();
                    String categoryName = (p.getCategory() != null && p.getCategory().getName() != null)
                            ? p.getCategory().getName().toLowerCase()
                            : "";
                    return lowerQ.contains(name)
                            || lowerQ.contains(brand)
                            || lowerQ.contains(categoryName);
                })
                .limit(5)
                .collect(Collectors.toList());

        // ✅ Dùng HashMap để tránh lỗi type mismatch
        List<Map<String, Object>> productList = filteredProducts.stream()
                .map(p -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", p.getId());
                    item.put("name", p.getName());
                    item.put("price", p.getPrice());
                    item.put("brand", p.getBrand());
                    item.put("category", p.getCategory() != null ? p.getCategory().getName() : "Không rõ");
                    return item;
                })
                .collect(Collectors.toList());
        
        // ✅ Gọi Gemini để tư vấn
        String response = geminiService.getProductAdvice(question, productList);

        // ✅ Kết thúc đo thời gian
        long end = System.currentTimeMillis();
        log.info("⚡ GeminiAdvisor xử lý xong trong {} ms ({} sản phẩm, câu hỏi: '{}')",
                (end - start), productList.size(), question);

        return response;
    }
}

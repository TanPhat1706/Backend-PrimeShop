package com.primeshop.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.primeshop.order.OrderItem;
import java.util.Set;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findById(Long id);

    Optional<Product> findByIdAndActiveTrue(Long id);

    // Optional<Product> findBySlug(String productSlug);
    Optional<Product> findBySlugAndActiveTrue(String productSlug);

    boolean existsBySlug(String slug);

    List<Product> findByCategorySlug(String categorySlug);

    List<Product> findByCategorySlugAndActiveTrue(String categorySlug);

    @EntityGraph(attributePaths = { "category" })
    @Query("SELECT p FROM Product p ORDER BY p.id ASC")
    List<Product> findAll();

    List<Product> findByActiveTrue();

    List<Product> findByActiveFalse();

    // @Query("SELECT p FROM Product p ORDER BY p.sold DESC AND p.active = true")
    // List<Product> findProductSoldDesc();

    List<Product> findByActiveTrueOrderBySoldDesc();

    @Query("SELECT DISTINCT p.brand FROM Product p")
    List<String> findDistinctBrands();

    // @Query("SELECT p FROM Product p WHERE p.isDiscounted = true AND p.active =
    // true ORDER BY p.discountPercent DESC")
    // List<Product> findProductDiscountDesc();

    List<Product> findByIsDiscountedTrueAndActiveTrueOrderByDiscountPercentDesc();

    Long countByActiveTrue();

    // Tìm sản phẩm cùng danh mục với các sản phẩm user đã mua,
    // và loại bỏ sản phẩm mà user đã mua rồi
    @Query("""
            SELECT p FROM Product p
            WHERE p.category.id IN :categoryIds
            AND p.id NOT IN (
                SELECT oi.product.id FROM OrderItem oi WHERE oi.order.user.id = :userId
            )
            AND p.active = true
            """)

    List<Product> findRecommendedProducts(@Param("categoryIds") Set<Long> categoryIds,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT oi2.product.id
            FROM OrderItem oi1
            JOIN OrderItem oi2 ON oi1.order.id = oi2.order.id
            WHERE oi1.product.id = :productId AND oi2.product.id <> :productId
            GROUP BY oi2.product.id
            ORDER BY COUNT(oi2.product.id) DESC
            """)
    List<Long> findFrequentlyBoughtTogether(@Param("productId") Long productId);
}

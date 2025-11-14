package com.primeshop.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findById(Long id);
    Optional<Product> findByIdAndActiveTrue(Long id);
    // Optional<Product> findBySlug(String productSlug);
    Optional<Product> findBySlugAndActiveTrue(String productSlug);
    boolean existsBySlug(String slug);
    List<Product> findByCategorySlug(String categorySlug);
    List<Product> findByCategorySlugAndActiveTrue(String categorySlug);
    List<Product> findAll();
    List<Product> findByActiveTrue();
    List<Product> findByActiveFalse();

    // @Query("SELECT p FROM Product p ORDER BY p.sold DESC AND p.active = true")
    // List<Product> findProductSoldDesc();

    List<Product> findByActiveTrueOrderBySoldDesc();

    @Query("SELECT DISTINCT p.brand FROM Product p")
    List<String> findDistinctBrands();

    // @Query("SELECT p FROM Product p WHERE p.isDiscounted = true AND p.active = true ORDER BY p.discountPercent DESC")
    // List<Product> findProductDiscountDesc();

    List<Product> findByIsDiscountedTrueAndActiveTrueOrderByDiscountPercentDesc();

    Long countByActiveTrue();
}

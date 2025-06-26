package com.primeshop.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.primeshop.cart.CartItem;
import com.primeshop.category.Category;
import com.primeshop.utils.CodeUtils;
import com.primeshop.utils.SlugUtils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "brand", length = 255)
    private String brand;
    
    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "discount_price", precision = 19, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "is_discounted", nullable = false)
    private Boolean isDiscounted = false;

    @Column(name = "rating", nullable = true)
    private Double rating = 0.0;

    @Column(name = "rating_count", nullable = true)
    private Integer ratingCount = 0;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "sold", nullable = false)
    private Integer sold = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "slug", unique = true, nullable = false, length = 255)
    private String slug;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSpec> specs = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<CartItem> cartItems;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductReview> reviews = new ArrayList<>();
    
    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Transient
    public String getCode() {
        return CodeUtils.encodeProductId(this.id);
    }

    @PrePersist
    public void onCreate() {
        if (this.slug == null || this.slug.isBlank()) {
            this.slug = SlugUtils.toSlug(this.name);            
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // updatedAt is handled by @UpdateTimestamp
    }

    public Product(ProductRequest request, Category category) {
        this.name = request.getName();
        this.brand = request.getBrand();
        this.price = request.getPrice();
        this.discountPercent = request.getDiscountPercent();
        this.discountPrice = calculateDiscountPrice(request.getPrice(), request.getDiscountPercent());
        this.isDiscounted = request.getDiscountPercent() != null && request.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0;
        this.imageUrl = request.getImageUrl();
        this.stock = request.getStock();
        this.sold = request.getSold() != null ? request.getSold() : 0;
        this.category = category;
        this.description = request.getDescription();
    }

    public BigDecimal calculateDiscountPrice(BigDecimal originalPrice, BigDecimal discountPercent) {
        if (originalPrice == null || discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) <= 0) {
            this.isDiscounted = false;
            return originalPrice;
        }
        this.isDiscounted = true;
        return originalPrice.multiply(BigDecimal.ONE.subtract(discountPercent.divide(new BigDecimal("100"))));
    }

    public void calculateAverageRating(List<Double> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            this.rating = 0.0;
            return;
        }
        double sum = ratings.stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        this.rating = sum / ratings.size();
    }

    public void addRating(Double newRating) {
        if (newRating < 1.0 || newRating > 5.0) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        if (this.rating == null) {
            this.rating = newRating;
            this.ratingCount = 1;
        } else {
            double totalRating = this.rating * this.ratingCount;
            this.ratingCount++;
            this.rating = Math.round(((totalRating + newRating) / this.ratingCount) * 10.0) / 10.0;
        }
    }

}

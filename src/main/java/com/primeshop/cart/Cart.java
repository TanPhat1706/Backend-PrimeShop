package com.primeshop.cart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.primeshop.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;   

    private BigDecimal totalPrice;

    public Cart(User user) {
        this.user = user;
        this.totalPrice = BigDecimal.ZERO;
        this.cartItems = new ArrayList<>();
    }

    public void setTotalPrice() {
        BigDecimal sum = this.cartItems.stream()
            .map(CartItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Round up to nearest thousand
        // this.totalPrice = sum.setScale(-3, BigDecimal.ROUND_UP);
        this.totalPrice = sum;
    }
}

package com.primeshop.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.primeshop.cart.Cart;
import com.primeshop.cart.CartItem;
import com.primeshop.cart.CartItemRepo;
import com.primeshop.cart.CartRepo;
import com.primeshop.product.Product;
import com.primeshop.product.ProductRepo;
import com.primeshop.user.User;
import com.primeshop.user.UserRepo;

import jakarta.transaction.Transactional;

@Service
public class OrderService {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private CartRepo cartRepo;
    @Autowired
    private CartItemRepo cartItemRepo;
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private OrderRepo orderRepo;


    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        //Lấy cart của user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found!"));

        Cart cart = cartRepo.findByUser(user).orElseThrow(() -> new RuntimeException("Cart is empty!"));

        List<CartItem> cartItems = cartItemRepo.findByCart(cart);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("No items in cart to order!");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Product '" + product.getName() + "' is out of stock.");
            }

            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepo.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(cartItem.getTotalPrice());

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);
        order.setFullName(request.getFullName());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setAddress(request.getAddress());
        if (request.getNote() != null) {
            order.setNote(request.getNote());
        } else {
            order.setNote("Không có");
        }

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(order);
        }

        orderRepo.save(order);
        cartItemRepo.deleteAll(cartItems);

        return new OrderResponse(order); 
    }

    public List<OrderResponse> getOrdersByUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found!"));
        return orderRepo.findByUser(user).stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long orderId) {        
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found!"));
        return new OrderResponse(order);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepo.findAll().stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> searchOrders(OrderFilterRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found!"));        
        
        Specification<Order> spec = OrderSpecification.filter(request, user);
        List<Order> orders = orderRepo.findAll(spec);
        return orders.stream().map(OrderResponse::new).collect(Collectors.toList());
    }

    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found!"));
        
        OrderStatus currentStatus = order.getStatus();

        if (!isValidStatusTransition(currentStatus, status)) {
            throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + status);
        }
        
        order.setStatus(status);
        orderRepo.save(order);
        return new OrderResponse(order);
    }

    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        switch (currentStatus) {
            case PENDING: return newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED: return newStatus == OrderStatus.PAID || newStatus == OrderStatus.PAYMENT_FAILED || newStatus == OrderStatus.CANCELLED;
            case PAID: return newStatus == OrderStatus.SHIPPED;
            case PAYMENT_FAILED: return newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case PROCESSING: return newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED: return newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED: return false;
            default: throw new IllegalArgumentException("Unknown status: " + currentStatus);
        }
    }

    public List<OrderResponse> filterOrders(OrderFilterRequest filterRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found!"));
        Specification<Order> spec = OrderSpecification.filter(filterRequest, user);
        List<Order> orders = orderRepo.findAll(spec);
        return orders.stream().map(OrderResponse::new).collect(Collectors.toList());
    }

    public Long countOrder() {
        return orderRepo.countByOrder();
    }

    public BigDecimal getTotalPurchase(Long userId) {
        return orderRepo.getTotalPurchase(OrderStatus.DELIVERED, userId);
    }

    public Long countByUser(Long userId) {
        return orderRepo.countByUser(userId);
    }
}

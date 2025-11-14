package com.primeshop.product;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

public class ProductSpecification {
    public static Specification<Product> filter(ProductFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            if (request.getSearch() != null) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + request.getSearch() + "%"));
            }
            if (request.getCategory() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("slug"), request.getCategory()));
            }
            if (request.getBrand() != null) {
                predicates.add(criteriaBuilder.equal(root.get("brand"), request.getBrand()));
            }
            if (request.getMinPrice() != null) {
                predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("price"), request.getMinPrice())
                );
            }            
            if (request.getMaxPrice() != null) {
                predicates.add(
                    criteriaBuilder.lessThanOrEqualTo(root.get("price"), request.getMaxPrice())
                );
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

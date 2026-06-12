package com.shopverse.infrastructure.jpa.specification;

import com.shopverse.infrastructure.jpa.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;

/**
 * Ch04-08: JPA Criteria API via Spring Specification — dynamic, type-safe queries.
 * Avoids string concatenation for optional filters.
 */
public class ProductSpecification {

    public static Specification<ProductEntity> hasCategory(String category) {
        return (root, query, cb) ->
            category == null || category.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("category"), category);
    }

    public static Specification<ProductEntity> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<ProductEntity> priceAtMost(java.math.BigDecimal max) {
        return (root, query, cb) ->
            max == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<ProductEntity> nameLike(String keyword) {
        return (root, query, cb) ->
            keyword == null || keyword.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }
}

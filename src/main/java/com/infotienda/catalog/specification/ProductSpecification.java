package com.infotienda.catalog.specification;

import com.infotienda.catalog.model.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> hasActive(Boolean active) {
        return (root, query, cb) -> active == null
                ? cb.conjunction()
                : cb.equal(root.get("active"), active);
    }

    public static Specification<Product> hasMinStock(Integer minStock) {
        return (root, query, cb) -> minStock == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("stockQuantity"), minStock);
    }

    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%");
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> categoryId == null
                ? cb.conjunction()
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasCategoryName(String categoryName) {
        return (root, query, cb) -> (categoryName == null || categoryName.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("category").get("name")), "%" + categoryName.trim().toLowerCase() + "%");
    }
}

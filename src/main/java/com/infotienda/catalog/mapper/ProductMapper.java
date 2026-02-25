package com.infotienda.catalog.mapper;

import com.infotienda.catalog.dto.ProductRequest;
import com.infotienda.catalog.dto.ProductResponse;
import com.infotienda.catalog.model.Category;
import com.infotienda.catalog.model.Product;
import com.infotienda.core.util.SlugUtil;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory().getName())
                .build();
    }

    public Product mapToProduct(ProductRequest productRequest, Category category, String imageUrl) {
        return Product.builder()
                .name(productRequest.getName())
                .category(category)
                .slug(SlugUtil.generateSlug(productRequest.getName()))
                .imageUrl(imageUrl)
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .stockQuantity(productRequest.getStockQuantity())
                .build();
    }
}

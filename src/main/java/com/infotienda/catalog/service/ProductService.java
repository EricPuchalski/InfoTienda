package com.infotienda.catalog.service;

import com.infotienda.catalog.dto.ProductRequest;
import com.infotienda.catalog.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {

    ProductResponse createProduct(ProductRequest productRequest, MultipartFile image);

    Page<ProductResponse> getProducts(
            int page,
            int size,
            String sortBy,
            String sortDir,
            boolean active,
            int minStock,
            String name,
            String categoryName
    );

    ProductResponse getProductById(Long id);

    void deleteProduct(Long id);

    ProductResponse updateProduct(Long id, ProductRequest productRequest, MultipartFile image);
}

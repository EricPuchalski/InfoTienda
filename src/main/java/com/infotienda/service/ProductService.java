package com.infotienda.service;

import com.infotienda.dto.ProductRequest;
import com.infotienda.dto.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest productRequest, MultipartFile image);

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    void deleteProduct(Long id);

    ProductResponse updateProduct(Long id, ProductRequest productRequest, MultipartFile image);
}
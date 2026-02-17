package com.infotienda.service.impl;

import com.infotienda.exception.ResourceNotFoundException;
import com.infotienda.mapper.ProductMapper;
import com.infotienda.model.Category;
import com.infotienda.model.Product;
import com.infotienda.dto.ProductRequest;
import com.infotienda.dto.ProductResponse;
import com.infotienda.repository.CategoryRepository;
import com.infotienda.repository.ProductRepository;
import java.util.List;
import java.util.stream.Collectors;

import com.infotienda.service.ProductService;
import com.infotienda.service.S3Service;
import com.infotienda.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final CategoryRepository categoryRepository;
    private final S3Service s3Service;

    public ProductResponse createProduct(ProductRequest productRequest, MultipartFile image) {
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequest.getCategoryId()));

        String imageUrl = s3Service.uploadFile(image);

        Product product = mapper.mapToProduct(productRequest, category, imageUrl);

        product.setSlug(SlugUtil.generateSlug(productRequest.getName()));

        Product savedProduct = productRepository.save(product);
        return mapper.mapToProductResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(mapper::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapper.mapToProductResponse(product);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        s3Service.deleteFile(product.getImageUrl());
        productRepository.delete(product);
    }

    public ProductResponse updateProduct(Long id, ProductRequest productRequest, MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequest.getCategoryId()));

        if (image != null && !image.isEmpty()) {
            s3Service.deleteFile(product.getImageUrl());
            String newImageUrl = s3Service.uploadFile(image);
            product.setImageUrl(newImageUrl);
        }

        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setStockQuantity(productRequest.getStockQuantity());
        product.setCategory(category);
        product.setSlug(SlugUtil.generateSlug(productRequest.getName()));

        Product updatedProduct = productRepository.save(product);
        return mapper.mapToProductResponse(updatedProduct);
    }
}

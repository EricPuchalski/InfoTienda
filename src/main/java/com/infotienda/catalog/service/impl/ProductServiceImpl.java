package com.infotienda.catalog.service.impl;

import com.infotienda.core.exception.ResourceNotFoundException;
import com.infotienda.catalog.mapper.ProductMapper;
import com.infotienda.catalog.model.Category;
import com.infotienda.catalog.model.Product;
import com.infotienda.catalog.dto.ProductRequest;
import com.infotienda.catalog.dto.ProductResponse;
import com.infotienda.catalog.repository.CategoryRepository;
import com.infotienda.catalog.repository.ProductRepository;
import com.infotienda.catalog.service.ProductService;
import com.infotienda.media.service.S3Service;
import com.infotienda.catalog.specification.ProductSpecification;
import com.infotienda.core.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final CategoryRepository categoryRepository;
    private final S3Service s3Service;

    @Override
    public ProductResponse createProduct(ProductRequest productRequest, MultipartFile image) {
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productRequest.getCategoryId()));

        String imageUrl = s3Service.uploadFile(image);

        Product product = mapper.mapToProduct(productRequest, category, imageUrl);

        product.setSlug(SlugUtil.generateSlug(productRequest.getName()));

        Product savedProduct = productRepository.save(product);
        return mapper.mapToProductResponse(savedProduct);
    }

    @Override
    public Page<ProductResponse> getProducts(
            int page,
            int size,
            String sortBy,
            String sortDir,
            boolean active,
            int minStock,
            String name,
            String categoryName
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = normalizeSortProperty(sortBy);

        Specification<Product> specification = Specification.where(ProductSpecification.hasActive(active))
                .and(ProductSpecification.hasMinStock(minStock))
                .and(ProductSpecification.nameContains(name))
                .and(ProductSpecification.hasCategoryName(categoryName));

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        return productRepository.findAll(specification, pageable)
                .map(mapper::mapToProductResponse);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapper.mapToProductResponse(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        s3Service.deleteFile(product.getImageUrl());
        productRepository.delete(product);
    }

    @Override
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

    private String normalizeSortProperty(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "price";
        }

        return switch (sortBy) {
            case "name", "price", "stockQuantity", "createdAt", "updatedAt" -> sortBy;
            default -> "price";
        };
    }
}

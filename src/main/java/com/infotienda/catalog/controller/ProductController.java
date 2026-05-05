package com.infotienda.catalog.controller;

import com.infotienda.catalog.dto.ProductRequest;
import com.infotienda.catalog.dto.ProductResponse;
import com.infotienda.catalog.service.ProductService;
import com.infotienda.core.util.JsonUtil;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;
    private final JsonUtil jsonUtil;

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("product") String productJson,
            @RequestPart("image") MultipartFile image) {
        ProductRequest productRequest = jsonUtil.parseProductRequest(productJson);
        return new ResponseEntity<>(productService.createProduct(productRequest, image), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "true") boolean active,
            @RequestParam(defaultValue = "1") @Min(0) int minStock,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryName) {
        return ResponseEntity.ok(productService.getProducts(
                page,
                size,
                sortBy,
                sortDir,
                active,
                minStock,
                name,
                categoryName
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        ProductRequest productRequest = jsonUtil.parseProductRequest(productJson);
        return ResponseEntity.ok(productService.updateProduct(id, productRequest, image));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }


}

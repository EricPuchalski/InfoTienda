package com.infotienda.service.impl;

import com.infotienda.dto.CategoryRequest;
import com.infotienda.dto.CategoryResponse;
import com.infotienda.exception.ResourceConflictException;
import com.infotienda.exception.ResourceNotFoundException;
import com.infotienda.model.Category;
import com.infotienda.repository.CategoryRepository;
import com.infotienda.repository.ProductRepository;
import com.infotienda.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        String normalizedName = normalizeName(request.getName());
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResourceConflictException("Category with name '" + normalizedName + "' already exists");
        }

        Category category = new Category();
        category.setName(normalizedName);
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return toResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String normalizedName = normalizeName(request.getName());
        categoryRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResourceConflictException("Category with name '" + normalizedName + "' already exists");
                });

        category.setName(normalizedName);
        Category updated = categoryRepository.save(category);
        return toResponse(updated);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (productRepository.countByCategoryId(id) > 0) {
            throw new ResourceConflictException("Cannot delete category with associated products");
        }

        categoryRepository.delete(category);
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}

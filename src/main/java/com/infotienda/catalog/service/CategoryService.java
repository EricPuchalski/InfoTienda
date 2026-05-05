package com.infotienda.catalog.service;

import com.infotienda.catalog.dto.CategoryRequest;
import com.infotienda.catalog.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}

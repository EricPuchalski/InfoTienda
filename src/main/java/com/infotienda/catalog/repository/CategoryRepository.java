package com.infotienda.catalog.repository;

import com.infotienda.catalog.model.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);

    Optional<Category> findByNameIgnoreCase(String name);
}

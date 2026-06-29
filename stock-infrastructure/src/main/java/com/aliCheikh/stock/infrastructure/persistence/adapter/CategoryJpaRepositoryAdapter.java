package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.port.CategoryRepository;
import com.aliCheikh.stock.infrastructure.persistence.mapper.CategoryJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class CategoryJpaRepositoryAdapter implements CategoryRepository {
    private final CategoryJpaRepository categoryJpaRepository;
    private final CategoryJpaMapper categoryJpaMapper;

    public CategoryJpaRepositoryAdapter(CategoryJpaRepository categoryJpaRepository,
                                        CategoryJpaMapper categoryJpaMapper) {
        this.categoryJpaRepository = Objects.requireNonNull(categoryJpaRepository, "categoryJpaRepository cannot be null");
        this.categoryJpaMapper = Objects.requireNonNull(categoryJpaMapper, "categoryJpaMapper cannot be null");
    }

    @Override
    public List<Category> findAll() {
        return this.categoryJpaRepository
                .findAll()
                .stream()
                .map(this.categoryJpaMapper::toDomain)
                .toList();
    }
}

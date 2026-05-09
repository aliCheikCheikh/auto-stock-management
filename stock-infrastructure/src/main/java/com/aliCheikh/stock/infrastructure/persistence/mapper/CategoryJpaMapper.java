package com.aliCheikh.stock.infrastructure.persistence.mapper;

import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CategoryJpaMapper {

    public Category toDomain(CategoryJpaEntity entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        return new Category(
                CategoryId.of(entity.getId()),
                entity.getName()
        );
    }

    public CategoryJpaEntity toEntity(Category category) {
        Objects.requireNonNull(category, "category cannot be null");

        return CategoryJpaEntity.of(
                category.getCategoryId().getValue(),
                category.getName()
        );
    }
}

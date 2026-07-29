package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.exception.category.DuplicateCategoryNameException;
import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.category.port.CategoryRepository;
import com.aliCheikh.stock.infrastructure.persistence.mapper.CategoryJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    @Override
    public Optional<Category> findById(CategoryId categoryId) {
        Objects.requireNonNull(categoryId, "categoryId cannot be null");
        return categoryJpaRepository.findById(categoryId.getValue()).map(categoryJpaMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Le contrôle d'unicité effectué en amont ne protège pas de deux créations simultanées :
     * l'index en base tranche alors, et l'erreur technique est traduite ici en erreur métier — sans
     * quoi l'appelant recevrait un 500 au lieu d'un 409.</p>
     */
    @Override
    public void save(Category category) {
        Objects.requireNonNull(category, "category cannot be null");

        try {
            categoryJpaRepository.saveAndFlush(categoryJpaMapper.toEntity(category));
        } catch (DataIntegrityViolationException violation) {
            throw new DuplicateCategoryNameException(category.getName());
        }
    }

    @Override
    public void delete(CategoryId categoryId) {
        Objects.requireNonNull(categoryId, "categoryId cannot be null");
        categoryJpaRepository.deleteById(categoryId.getValue());
    }

    @Override
    public boolean existsByName(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return categoryJpaRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameExcluding(String name, CategoryId excludedCategoryId) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(excludedCategoryId, "excludedCategoryId cannot be null");
        return categoryJpaRepository.existsByNameIgnoreCaseAndIdNot(name, excludedCategoryId.getValue());
    }
}

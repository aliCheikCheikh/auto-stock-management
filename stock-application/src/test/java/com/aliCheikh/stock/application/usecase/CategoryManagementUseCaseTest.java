package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.category.CategoryInUseException;
import com.aliCheikh.stock.domain.exception.category.CategoryNotFoundException;
import com.aliCheikh.stock.domain.exception.category.DuplicateCategoryNameException;
import com.aliCheikh.stock.domain.exception.category.InvalidCategoryNameException;
import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.category.port.CategoryRepository;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Category management use cases. */
public class CategoryManagementUseCaseTest {

    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;
    private CreateCategoryUseCase createCategory;
    private RenameCategoryUseCase renameCategory;
    private DeleteCategoryUseCase deleteCategory;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        productRepository = mock(ProductRepository.class);
        TransactionRunner transactionRunner = new TransactionRunner() {
            @Override
            public <T> T execute(Supplier<T> work) {
                return work.get();
            }
        };
        createCategory = new CreateCategoryUseCase(categoryRepository, transactionRunner);
        renameCategory = new RenameCategoryUseCase(categoryRepository, transactionRunner);
        deleteCategory = new DeleteCategoryUseCase(categoryRepository, productRepository, transactionRunner);
    }

    // Creation

    @Test
    public void should_create_a_category_with_a_trimmed_name() {
        given(categoryRepository.existsByName(any())).willReturn(false);

        Category created = createCategory.create("  Freinage  ");

        assertThat(created.getName()).isEqualTo("Freinage");
        verify(categoryRepository).save(created);
    }

    @Test
    public void should_check_uniqueness_on_the_normalized_name() {
        given(categoryRepository.existsByName(any())).willReturn(false);

        createCategory.create("  Freinage  ");

        // Check uniqueness against the normalized value that will be stored.
        ArgumentCaptor<String> checked = ArgumentCaptor.forClass(String.class);
        verify(categoryRepository).existsByName(checked.capture());
        assertThat(checked.getValue()).isEqualTo("Freinage");
    }

    @Test
    public void should_reject_a_name_already_taken() {
        given(categoryRepository.existsByName("Freinage")).willReturn(true);

        assertThatThrownBy(() -> createCategory.create("Freinage"))
                .isInstanceOf(DuplicateCategoryNameException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    public void should_reject_a_blank_name() {
        assertThatThrownBy(() -> createCategory.create("   "))
                .isInstanceOf(InvalidCategoryNameException.class);

        verify(categoryRepository, never()).save(any());
    }

    // --- Renommage ---

    @Test
    public void should_rename_a_category() {
        CategoryId id = CategoryId.generate();
        given(categoryRepository.findById(id)).willReturn(Optional.of(new Category(id, "Freinaje")));
        given(categoryRepository.existsByNameExcluding(any(), any())).willReturn(false);

        Category renamed = renameCategory.rename(id, "Freinage");

        assertThat(renamed.getName()).isEqualTo("Freinage");
        verify(categoryRepository).save(renamed);
    }

    @Test
    public void should_exclude_the_renamed_category_from_the_uniqueness_check() {
        CategoryId id = CategoryId.generate();
        given(categoryRepository.findById(id)).willReturn(Optional.of(new Category(id, "freinage")));
        given(categoryRepository.existsByNameExcluding(any(), any())).willReturn(false);

        // A case-only rename is not a duplicate.
        renameCategory.rename(id, "Freinage");

        verify(categoryRepository).existsByNameExcluding("Freinage", id);
    }

    @Test
    public void should_reject_a_rename_colliding_with_another_category() {
        CategoryId id = CategoryId.generate();
        given(categoryRepository.findById(id)).willReturn(Optional.of(new Category(id, "Moteur")));
        given(categoryRepository.existsByNameExcluding("Freinage", id)).willReturn(true);

        assertThatThrownBy(() -> renameCategory.rename(id, "Freinage"))
                .isInstanceOf(DuplicateCategoryNameException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    public void should_fail_to_rename_an_unknown_category() {
        CategoryId unknown = CategoryId.generate();
        given(categoryRepository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> renameCategory.rename(unknown, "Freinage"))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    // --- Suppression ---

    @Test
    public void should_delete_an_empty_category() {
        CategoryId id = CategoryId.generate();
        given(categoryRepository.findById(id)).willReturn(Optional.of(new Category(id, "Obsolète")));
        given(productRepository.existsByCategoryId(id)).willReturn(false);

        deleteCategory.delete(id);

        verify(categoryRepository).delete(id);
    }

    @Test
    public void should_refuse_to_delete_a_category_still_holding_products() {
        CategoryId id = CategoryId.generate();
        given(categoryRepository.findById(id)).willReturn(Optional.of(new Category(id, "Freinage")));
        given(productRepository.existsByCategoryId(id)).willReturn(true);

        // Products must be reassigned before deleting their category.
        assertThatThrownBy(() -> deleteCategory.delete(id))
                .isInstanceOf(CategoryInUseException.class);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    public void should_fail_to_delete_an_unknown_category() {
        CategoryId unknown = CategoryId.generate();
        given(categoryRepository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> deleteCategory.delete(unknown))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }
}

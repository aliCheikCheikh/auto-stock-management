package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class StockReceiptImportCategoryQueryJpaAdapterTest {

    @Test
    void resolves_category_names_in_one_query() {
        CategoryJpaRepository repository = mock(CategoryJpaRepository.class);
        var adapter = new StockReceiptImportCategoryQueryJpaAdapter(repository);
        UUID categoryId = UUID.randomUUID();
        given(repository.findByNormalizedNames(Set.of("freinage"))).willReturn(List.of(
                CategoryJpaEntity.of(categoryId, "Freinage")
        ));

        var candidates = adapter.findByNames(Set.of("freinage"));

        assertThat(candidates).singleElement().satisfies(candidate -> {
            assertThat(candidate.categoryId().getValue()).isEqualTo(categoryId);
            assertThat(candidate.name()).isEqualTo("Freinage");
        });
    }

    @Test
    void avoids_database_access_when_no_category_is_requested() {
        CategoryJpaRepository repository = mock(CategoryJpaRepository.class);
        var adapter = new StockReceiptImportCategoryQueryJpaAdapter(repository);

        assertThat(adapter.findByNames(Set.of())).isEmpty();

        verifyNoInteractions(repository);
    }
}

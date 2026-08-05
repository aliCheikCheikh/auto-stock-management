package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class StockReceiptImportProductQueryJpaAdapterTest {

    @Test
    void resolves_references_and_names_in_bulk_without_duplicate_candidates() {
        ProductJpaRepository repository = mock(ProductJpaRepository.class);
        var adapter = new StockReceiptImportProductQueryJpaAdapter(repository);
        ProductJpaEntity sameProduct = product("REF-001", "Filtre", true);
        ProductJpaEntity nameConflict = product("REF-002", "Rotule", false);
        given(repository.findByNormalizedReferences(Set.of("ref-001"))).willReturn(List.of(sameProduct));
        given(repository.findByNormalizedNames(Set.of("filtre", "rotule")))
                .willReturn(List.of(sameProduct, nameConflict));

        var candidates = adapter.findCandidates(
                Set.of("ref-001"),
                Set.of("filtre", "rotule")
        );

        assertThat(candidates).hasSize(2);
        assertThat(candidates)
                .extracting(candidate -> candidate.reference())
                .containsExactly("REF-001", "REF-002");
        assertThat(candidates.get(1).active()).isFalse();
    }

    @Test
    void avoids_database_access_when_no_candidate_is_requested() {
        ProductJpaRepository repository = mock(ProductJpaRepository.class);
        var adapter = new StockReceiptImportProductQueryJpaAdapter(repository);

        assertThat(adapter.findCandidates(Set.of(), Set.of())).isEmpty();

        verifyNoInteractions(repository);
    }

    private ProductJpaEntity product(String reference, String name, boolean active) {
        return ProductJpaEntity.of(
                UUID.randomUUID(),
                name,
                reference,
                UUID.randomUUID(),
                0,
                new BigDecimal("1000.00"),
                "XAF",
                active
        );
    }
}

package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.application.dto.ProductInfo;
import com.aliCheikh.stock.application.dto.ReceiveStockCommand;
import com.aliCheikh.stock.application.dto.TargetLocation;
import com.aliCheikh.stock.application.usecase.ReceiveStockUseCase;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ShopJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StorageLocationJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ShopJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.StorageLocationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prouve l'ATOMICITÉ de la réception : une réception qui crée un produit puis
 * échoue plus loin ne doit laisser AUCUNE trace (rollback complet).
 *
 * On utilise {@code @SpringBootTest} (et surtout PAS {@code @DataJpaTest}) :
 * ce dernier envelopperait chaque test dans sa propre transaction annulée en fin
 * de test, ce qui masquerait la transaction réelle du use case. Ici, la
 * transaction ouverte par SpringTransactionRunner est réelle et indépendante, et
 * on interroge la base depuis l'extérieur.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReceiveStockAtomicityTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("auto_stock_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ReceiveStockUseCase receiveStockUseCase;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Autowired
    private ShopJpaRepository shopJpaRepository;

    @Autowired
    private StorageLocationJpaRepository storageLocationJpaRepository;

    private CategoryId categoryId;
    private ShopId shopId;
    private LocationId shopFloorId;

    @BeforeEach
    void setUp() {
        categoryId = CategoryId.generate();
        shopId = ShopId.generate();
        shopFloorId = LocationId.generate();

        // Catégorie / magasin / emplacement valides (nom de catégorie unique pour
        // ne pas heurter les catégories semées par Flyway V7).
        categoryJpaRepository.save(CategoryJpaEntity.of(
                categoryId.getValue(), "Atomicité-" + categoryId.getValue()));
        shopJpaRepository.save(ShopJpaEntity.of(
                shopId.getValue(), "Magasin Atomicité", "N'Djamena"));
        storageLocationJpaRepository.save(StorageLocationJpaEntity.of(
                shopFloorId.getValue(), shopId.getValue(), LocationType.SHOP_FLOOR, "Surface de vente", 0));
    }

    @Test
    void a_reception_that_fails_midway_leaves_no_trace() {
        String reference = "ATOMIC-" + UUID.randomUUID();

        // userId absent de app_user : la clé étrangère performed_by du mouvement
        // explose au commit, APRÈS que le nouveau produit a été créé dans la même
        // transaction. C'est exactement le scénario du bug d'origine.
        UserId ghostUser = UserId.of(UUID.randomUUID());

        ReceiveStockCommand command = new ReceiveStockCommand(
                reference,
                new ProductInfo(
                        "Produit Atomique",
                        reference,
                        categoryId,
                        Money.create(new BigDecimal("2500.00"), Currency.getInstance("XAF")),
                        5
                ),
                shopId,
                ghostUser,
                List.of(new TargetLocation(shopFloorId, 10))
        );

        // La réception doit échouer (violation de contrainte au commit).
        assertThatThrownBy(() -> receiveStockUseCase.execute(command))
                .isInstanceOf(RuntimeException.class);

        // Preuve d'atomicité : le produit créé en cours de route a été ANNULÉ.
        assertThat(productJpaRepository.findByReference(reference))
                .as("un échec en cours de réception ne doit laisser aucun produit")
                .isEmpty();
    }
}

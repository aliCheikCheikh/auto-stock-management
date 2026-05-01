package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.application.dto.TransferStockCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class TransferStockUseCaseTest {

    private StorageLocationRepository storageLocationRepository;
    private StockMovementRepository stockMovementRepository;
    private EventPublisher eventPublisher;

    private TransferStockUseCase transferStockUseCase;

    @BeforeEach
    public void setUp() {
        storageLocationRepository = mock(StorageLocationRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        eventPublisher = mock(EventPublisher.class);

        transferStockUseCase = new TransferStockUseCase(
                storageLocationRepository, stockMovementRepository, eventPublisher
        );
    }

    @Test
    public void should_transfer_stock_successfully_between_two_locations() {
        // GIVEN
        ProductId productId = ProductId.generate();
        LocationId reserveId = LocationId.generate();
        LocationId magasinId = LocationId.generate();
        UserId userId = UserId.generate();
        int transferQuantity = 10;

        TransferStockCommand command = new TransferStockCommand(
                productId, reserveId, magasinId, transferQuantity, userId
        );

        // On mocke les entités StorageLocation pour vérifier qu'on appelle bien leurs méthodes
        StorageLocation reserve = mock(StorageLocation.class);
        StorageLocation magasin = mock(StorageLocation.class);

        when(storageLocationRepository.findById(reserveId)).thenReturn(Optional.of(reserve));
        when(storageLocationRepository.findById(magasinId)).thenReturn(Optional.of(magasin));

        // Phase 1 : Validation (La réserve a assez de stock)
        when(reserve.hasEnoughStock(productId, transferQuantity)).thenReturn(true);

        // WHEN
        transferStockUseCase.execute(command);

        // THEN
        // Vérification de la Phase 1 & 2 : Logique sur les agrégats
        verify(reserve).hasEnoughStock(productId, transferQuantity);
        verify(reserve).decreaseStock(productId, transferQuantity);
        verify(magasin).increaseStock(productId, transferQuantity);

        verify(storageLocationRepository).save(reserve);
        verify(storageLocationRepository).save(magasin);

        // Vérification de la Phase 3 : Création et sauvegarde du mouvement
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());

        StockMovement savedMovement = movementCaptor.getValue();
        assertThat(savedMovement.getProductId()).isEqualTo(productId);
        assertThat(savedMovement.getQuantity()).isEqualTo(transferQuantity);

        // On "ouvre la boîte" des Optionals grâce à aux getters robustes !
        assertThat(savedMovement.getSourceLocationId()).isPresent().contains(reserveId);
        assertThat(savedMovement.getDestinationLocationId()).isPresent().contains(magasinId);

        // On vérifie qu'aucun événement global n'a fuité
        verify(eventPublisher, never()).publish(anyList());
    }
}
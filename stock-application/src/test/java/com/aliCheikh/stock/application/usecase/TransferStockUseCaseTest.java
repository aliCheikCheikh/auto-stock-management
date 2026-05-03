package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.TransferStockCommand;
import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStockTransferException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStockTransferReason;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransferStockUseCaseTest {

    private StorageLocationRepository storageLocationRepository;
    private StockMovementRepository stockMovementRepository;
    private EventPublisher eventPublisher;

    private TransferStockUseCase transferStockUseCase;

    @BeforeEach
    void setUp() {
        storageLocationRepository = mock(StorageLocationRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        eventPublisher = mock(EventPublisher.class);

        transferStockUseCase = new TransferStockUseCase(
                storageLocationRepository,
                stockMovementRepository,
                eventPublisher
        );
    }

    @Test
    void should_transfer_stock_from_backstock_to_shop_floor_with_partial_source_depletion() {
        TransferFixture fixture = givenValidTransfer(10, 2, 4);

        transferStockUseCase.execute(fixture.command());

        assertThat(fixture.source().getStockLevel(fixture.productId())).isEqualTo(6);
        assertThat(fixture.destination().getStockLevel(fixture.productId())).isEqualTo(6);

        assertSavedTransferMovement(fixture, 4);
        assertLocationsAreSavedBeforeMovement(fixture.source(), fixture.destination());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_transfer_stock_from_backstock_to_shop_floor_with_full_source_depletion() {
        TransferFixture fixture = givenValidTransfer(10, 2, 10);

        transferStockUseCase.execute(fixture.command());

        assertThat(fixture.source().getStockLevel(fixture.productId())).isEqualTo(0);
        assertThat(fixture.destination().getStockLevel(fixture.productId())).isEqualTo(12);

        assertSavedTransferMovement(fixture, 10);
        assertLocationsAreSavedBeforeMovement(fixture.source(), fixture.destination());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_reject_transfer_when_source_does_not_have_enough_stock() {
        TransferFixture fixture = givenValidTransfer(8, 2, 10);

        assertThatThrownBy(() -> transferStockUseCase.execute(fixture.command()))
                .isInstanceOf(InsufficientStockException.class)
                .satisfies(exception -> {
                    InsufficientStockException stockException = (InsufficientStockException) exception;

                    assertThat(stockException.getProductId()).isEqualTo(fixture.productId());
                    assertThat(stockException.getAvailableQuantity()).isEqualTo(8);
                    assertThat(stockException.getRequestedQuantity()).isEqualTo(10);
                });

        assertThat(fixture.source().getStockLevel(fixture.productId())).isEqualTo(8);
        assertThat(fixture.destination().getStockLevel(fixture.productId())).isEqualTo(2);

        verify(storageLocationRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_reject_transfer_between_different_shops() {
        ProductId productId = ProductId.generate();
        UserId userId = UserId.generate();

        StorageLocation source = storageLocation(
                ShopId.generate(),
                LocationType.BACKSTOCK,
                "Backstock"
        );

        StorageLocation destination = storageLocation(
                ShopId.generate(),
                LocationType.SHOP_FLOOR,
                "Shop floor"
        );

        source.increaseStock(productId, 10);
        destination.increaseStock(productId, 2);

        TransferStockCommand command = new TransferStockCommand(
                productId,
                source.getLocationId(),
                destination.getLocationId(),
                4,
                userId
        );

        givenLocationsExist(source, destination);

        assertThatThrownBy(() -> transferStockUseCase.execute(command))
                .isInstanceOf(InvalidStockTransferException.class)
                .satisfies(exception -> {
                    InvalidStockTransferException transferException =
                            (InvalidStockTransferException) exception;

                    assertThat(transferException.getReason())
                            .isEqualTo(InvalidStockTransferReason.CROSS_SHOP_TRANSFER);
                    assertThat(transferException.getSourceLocationId())
                            .isEqualTo(source.getLocationId());
                    assertThat(transferException.getDestinationLocationId())
                            .isEqualTo(destination.getLocationId());
                });

        assertThat(source.getStockLevel(productId)).isEqualTo(10);
        assertThat(destination.getStockLevel(productId)).isEqualTo(2);

        verify(storageLocationRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_reject_transfer_when_source_is_not_backstock() {
        TransferFixture fixture = givenTransfer(
                LocationType.SHOP_FLOOR,
                LocationType.SHOP_FLOOR,
                10,
                2,
                4
        );

        assertThatThrownBy(() -> transferStockUseCase.execute(fixture.command()))
                .isInstanceOf(InvalidStockTransferException.class)
                .satisfies(exception -> {
                    InvalidStockTransferException transferException =
                            (InvalidStockTransferException) exception;

                    assertThat(transferException.getReason())
                            .isEqualTo(InvalidStockTransferReason.INVALID_TRANSFER_DIRECTION);
                });

        assertThat(fixture.source().getStockLevel(fixture.productId())).isEqualTo(10);
        assertThat(fixture.destination().getStockLevel(fixture.productId())).isEqualTo(2);

        verify(storageLocationRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_reject_transfer_when_destination_is_not_shop_floor() {
        TransferFixture fixture = givenTransfer(
                LocationType.BACKSTOCK,
                LocationType.BACKSTOCK,
                10,
                2,
                4
        );

        assertThatThrownBy(() -> transferStockUseCase.execute(fixture.command()))
                .isInstanceOf(InvalidStockTransferException.class)
                .satisfies(exception -> {
                    InvalidStockTransferException transferException =
                            (InvalidStockTransferException) exception;

                    assertThat(transferException.getReason())
                            .isEqualTo(InvalidStockTransferReason.INVALID_TRANSFER_DIRECTION);
                });

        assertThat(fixture.source().getStockLevel(fixture.productId())).isEqualTo(10);
        assertThat(fixture.destination().getStockLevel(fixture.productId())).isEqualTo(2);

        verify(storageLocationRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    private TransferFixture givenValidTransfer(
            int initialSourceQuantity,
            int initialDestinationQuantity,
            int transferQuantity
    ) {
        return givenTransfer(
                LocationType.BACKSTOCK,
                LocationType.SHOP_FLOOR,
                initialSourceQuantity,
                initialDestinationQuantity,
                transferQuantity
        );
    }

    private TransferFixture givenTransfer(
            LocationType sourceType,
            LocationType destinationType,
            int initialSourceQuantity,
            int initialDestinationQuantity,
            int transferQuantity
    ) {
        ShopId shopId = ShopId.generate();
        ProductId productId = ProductId.generate();
        UserId userId = UserId.generate();

        StorageLocation source = storageLocation(shopId, sourceType, "Source");
        StorageLocation destination = storageLocation(shopId, destinationType, "Destination");

        source.increaseStock(productId, initialSourceQuantity);
        destination.increaseStock(productId, initialDestinationQuantity);

        TransferStockCommand command = new TransferStockCommand(
                productId,
                source.getLocationId(),
                destination.getLocationId(),
                transferQuantity,
                userId
        );

        givenLocationsExist(source, destination);

        return new TransferFixture(productId, userId, source, destination, command);
    }

    private StorageLocation storageLocation(ShopId shopId, LocationType locationType, String label) {
        return new StorageLocation(
                LocationId.generate(),
                shopId,
                locationType,
                label,
                3
        );
    }

    private void givenLocationsExist(StorageLocation source, StorageLocation destination) {
        when(storageLocationRepository.findById(source.getLocationId()))
                .thenReturn(Optional.of(source));
        when(storageLocationRepository.findById(destination.getLocationId()))
                .thenReturn(Optional.of(destination));
    }

    private void assertSavedTransferMovement(TransferFixture fixture, int expectedQuantity) {
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());

        StockMovement savedMovement = movementCaptor.getValue();

        assertThat(savedMovement.getMovementType()).isEqualTo(MovementType.TRANSFER);
        assertThat(savedMovement.getProductId()).isEqualTo(fixture.productId());
        assertThat(savedMovement.getQuantity()).isEqualTo(expectedQuantity);
        assertThat(savedMovement.getPerformedBy()).isEqualTo(fixture.userId());
        assertThat(savedMovement.getSourceLocationId())
                .isPresent()
                .contains(fixture.source().getLocationId());
        assertThat(savedMovement.getDestinationLocationId())
                .isPresent()
                .contains(fixture.destination().getLocationId());
        assertThat(savedMovement.getSaleId()).isEmpty();
    }

    private void assertLocationsAreSavedBeforeMovement(
            StorageLocation source,
            StorageLocation destination
    ) {
        InOrder inOrder = inOrder(storageLocationRepository, stockMovementRepository);

        inOrder.verify(storageLocationRepository).save(source);
        inOrder.verify(storageLocationRepository).save(destination);
        inOrder.verify(stockMovementRepository).save(any(StockMovement.class));
    }

    private record TransferFixture(
            ProductId productId,
            UserId userId,
            StorageLocation source,
            StorageLocation destination,
            TransferStockCommand command
    ) {
    }
}
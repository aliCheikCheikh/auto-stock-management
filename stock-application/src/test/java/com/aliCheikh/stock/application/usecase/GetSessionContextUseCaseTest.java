package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ContextLocationView;
import com.aliCheikh.stock.application.dto.ContextView;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetSessionContextUseCaseTest {

    private StorageLocationRepository storageLocationRepository;
    private GetSessionContextUseCase getSessionContextUseCase;

    @BeforeEach
    void setUp() {
        storageLocationRepository = mock(StorageLocationRepository.class);
        getSessionContextUseCase = new GetSessionContextUseCase(storageLocationRepository);
    }

    @Test
    void should_build_the_context_from_the_single_shop_locations() {
        ShopId shopId = ShopId.generate();
        LocationId shopFloorId = LocationId.generate();
        LocationId backstockId = LocationId.generate();

        StorageLocation shopFloor = new StorageLocation(
                shopFloorId, shopId, LocationType.SHOP_FLOOR, "Surface de vente", 0);
        StorageLocation backstock = new StorageLocation(
                backstockId, shopId, LocationType.BACKSTOCK, "Réserve", 0);

        when(storageLocationRepository.findAll()).thenReturn(List.of(shopFloor, backstock));

        ContextView context = getSessionContextUseCase.execute();

        assertThat(context.shopId()).isEqualTo(shopId);
        assertThat(context.locations())
                .extracting(ContextLocationView::type)
                .containsExactlyInAnyOrder(LocationType.SHOP_FLOOR, LocationType.BACKSTOCK);
        assertThat(context.locations())
                .extracting(ContextLocationView::locationId)
                .containsExactlyInAnyOrder(shopFloorId, backstockId);
        assertThat(context.locations())
                .extracting(ContextLocationView::label)
                .containsExactlyInAnyOrder("Surface de vente", "Réserve");
    }

    @Test
    void should_fail_when_no_location_is_provisioned() {
        when(storageLocationRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> getSessionContextUseCase.execute())
                .isInstanceOf(IllegalStateException.class);
    }
}

package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ContextLocationView;
import com.aliCheikh.stock.application.dto.ContextView;
import com.aliCheikh.stock.application.usecase.GetSessionContextUseCase;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContextController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSessionContextUseCase getSessionContextUseCase;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @Test
    void should_return_the_session_context() throws Exception {
        UUID shopId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID shopFloorId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID backstockId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        given(getSessionContextUseCase.execute()).willReturn(new ContextView(
                ShopId.of(shopId),
                List.of(
                        new ContextLocationView(LocationType.SHOP_FLOOR, LocationId.of(shopFloorId), "Surface de vente"),
                        new ContextLocationView(LocationType.BACKSTOCK, LocationId.of(backstockId), "Réserve")
                )
        ));

        mockMvc.perform(get("/api/v1/context"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shopId").value(shopId.toString()))
                .andExpect(jsonPath("$.locations.length()").value(2))
                .andExpect(jsonPath("$.locations[0].type").value("SHOP_FLOOR"))
                .andExpect(jsonPath("$.locations[0].locationId").value(shopFloorId.toString()))
                .andExpect(jsonPath("$.locations[0].label").value("Surface de vente"))
                .andExpect(jsonPath("$.locations[1].type").value("BACKSTOCK"))
                .andExpect(jsonPath("$.locations[1].locationId").value(backstockId.toString()))
                .andExpect(jsonPath("$.locations[1].label").value("Réserve"));
    }
}

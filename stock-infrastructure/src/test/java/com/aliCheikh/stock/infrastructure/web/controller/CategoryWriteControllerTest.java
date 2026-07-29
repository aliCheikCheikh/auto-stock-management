package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.usecase.CreateCategoryUseCase;
import com.aliCheikh.stock.application.usecase.DeleteCategoryUseCase;
import com.aliCheikh.stock.application.usecase.ListCategoriesUseCase;
import com.aliCheikh.stock.application.usecase.RenameCategoryUseCase;
import com.aliCheikh.stock.domain.exception.category.CategoryInUseException;
import com.aliCheikh.stock.domain.exception.category.CategoryNotFoundException;
import com.aliCheikh.stock.domain.exception.category.DuplicateCategoryNameException;
import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Gestion des familles de pièces — tranche web. */
@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryWriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListCategoriesUseCase listCategoriesUseCase;

    @MockitoBean
    private CreateCategoryUseCase createCategoryUseCase;

    @MockitoBean
    private RenameCategoryUseCase renameCategoryUseCase;

    @MockitoBean
    private DeleteCategoryUseCase deleteCategoryUseCase;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @Test
    void should_create_a_category() throws Exception {
        CategoryId id = CategoryId.generate();
        given(createCategoryUseCase.create(any())).willReturn(new Category(id, "Freinage"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Freinage\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.getValue().toString()))
                .andExpect(jsonPath("$.name").value("Freinage"));
    }

    @Test
    void should_reject_a_blank_name_before_reaching_the_domain() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_409_when_the_name_is_already_used() throws Exception {
        given(createCategoryUseCase.create(any()))
                .willThrow(new DuplicateCategoryNameException("Freinage"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Freinage\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_ALREADY_USED"));
    }

    @Test
    void should_rename_a_category() throws Exception {
        CategoryId id = CategoryId.generate();
        given(renameCategoryUseCase.rename(any(), any())).willReturn(new Category(id, "Freinage"));

        mockMvc.perform(put("/api/v1/categories/{id}", id.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Freinage\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Freinage"));
    }

    @Test
    void should_return_404_when_renaming_an_unknown_category() throws Exception {
        CategoryId id = CategoryId.generate();
        given(renameCategoryUseCase.rename(any(), any()))
                .willThrow(new CategoryNotFoundException(id));

        mockMvc.perform(put("/api/v1/categories/{id}", id.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Freinage\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void should_delete_an_empty_category() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/categories/{id}", id))
                .andExpect(status().isNoContent());

        verify(deleteCategoryUseCase).delete(CategoryId.of(id));
    }

    @Test
    void should_return_409_when_deleting_a_category_still_in_use() throws Exception {
        CategoryId id = CategoryId.generate();
        willThrow(new CategoryInUseException(id)).given(deleteCategoryUseCase).delete(any());

        mockMvc.perform(delete("/api/v1/categories/{id}", id.getValue()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_IN_USE"));
    }
}

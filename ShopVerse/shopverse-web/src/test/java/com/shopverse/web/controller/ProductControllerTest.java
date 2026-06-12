package com.shopverse.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopverse.test.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ProductController — CRUD")
class ProductControllerTest extends BaseIntegrationTest {

    @Autowired ObjectMapper objectMapper;

    @Value("${shopverse.admin.secret}")
    private String adminSecret;

    private String adminJwt;
    private Long   productId;

    @BeforeEach
    void setUp() throws Exception {
        adminJwt = adminToken("admin@shopverse.com");

        // Create one product to use across tests
        String response = mockMvc.perform(post("/api/products")
                        .header("Authorization", adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",          "Test Widget",
                                "description",   "A test widget",
                                "price",         "29.99",
                                "currency",      "USD",
                                "category",      "Electronics",
                                "stockQuantity", 100))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        productId = objectMapper.readTree(response).path("data").path("id").longValue();
    }

    @Test
    @DisplayName("GET /api/products/{id} → 200 with product data")
    void get_product_by_id() throws Exception {
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("Test Widget"))
                .andExpect(jsonPath("$.data.price").value(29.99));
    }

    @Test
    @DisplayName("GET /api/products/{id} → 404 for non-existent product")
    void get_product_not_found() throws Exception {
        mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/products → 200 with product list (public)")
    void list_products_public() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("POST /api/products → 201 when admin")
    void create_product_as_admin() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",          "New Gadget",
                                "description",   "Brand new",
                                "price",         "9.99",
                                "currency",      "USD",
                                "category",      "Accessories",
                                "stockQuantity", 50))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("New Gadget"));
    }

    @Test
    @DisplayName("POST /api/products → 403 when not admin")
    void create_product_as_user_forbidden() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", userToken("user@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",          "Sneaky Product",
                                "description",   "Should not work",
                                "price",         "1.00",
                                "currency",      "USD",
                                "category",      "Other",
                                "stockQuantity", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/products → 401 when unauthenticated")
    void create_product_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Widget", "description", "d",
                                "price", "1.00", "currency", "USD",
                                "category", "X", "stockQuantity", 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/products/{id} → 200 updates product")
    void update_product_as_admin() throws Exception {
        mockMvc.perform(put("/api/products/" + productId)
                        .header("Authorization", adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",          "Updated Widget",
                                "description",   "Updated description",
                                "price",         "39.99",
                                "currency",      "USD",
                                "category",      "Electronics",
                                "stockQuantity", 80))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Widget"))
                .andExpect(jsonPath("$.data.price").value(39.99));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} → 200 removes product")
    void delete_product_as_admin() throws Exception {
        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", adminJwt))
                .andExpect(status().isOk());

        // Product should now be gone (or inactive)
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/products → 400 when required fields missing")
    void create_product_missing_fields() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

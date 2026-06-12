package com.shopverse.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopverse.test.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CustomerController")
class CustomerControllerTest extends BaseIntegrationTest {

    @Autowired ObjectMapper objectMapper;

    private Long   customerId;
    private String userJwt;

    @BeforeEach
    void setUp() throws Exception {
        String resp = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Dana",
                                "lastName",  "White",
                                "email",     "dana@test.com",
                                "password",  "pass123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        customerId = objectMapper.readTree(resp).path("data").path("customerId").longValue();
        userJwt    = "Bearer " + objectMapper.readTree(resp).path("data").path("token").asText();
    }

    @Test
    @DisplayName("GET /api/customers/{id} → 200 returns customer data")
    void get_customer_returns_200() throws Exception {
        mockMvc.perform(get("/api/customers/" + customerId)
                        .header("Authorization", userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(customerId))
                .andExpect(jsonPath("$.data.email").value("dana@test.com"))
                .andExpect(jsonPath("$.data.fullName").value("Dana White"));
    }

    @Test
    @DisplayName("GET /api/customers/{id} → 401 when unauthenticated")
    void get_customer_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/customers/" + customerId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/customers/{id} → 404 for non-existent customer")
    void get_customer_not_found() throws Exception {
        mockMvc.perform(get("/api/customers/99999")
                        .header("Authorization", userJwt))
                .andExpect(status().isNotFound());
    }
}

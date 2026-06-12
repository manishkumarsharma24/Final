package com.shopverse.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopverse.test.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController — register & login")
class AuthControllerTest extends BaseIntegrationTest {

    @Autowired ObjectMapper objectMapper;

    @Value("${shopverse.admin.secret}")
    private String adminSecret;

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register → 201 with JWT token")
    void register_user_returns_201_and_token() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Alice",
                                "lastName",  "Smith",
                                "email",     "alice@test.com",
                                "password",  "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("alice@test.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/register → 400 when email is blank")
    void register_with_blank_email_returns_400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Alice",
                                "lastName",  "Smith",
                                "email",     "",
                                "password",  "secret"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register → 400 or 409 when email already registered")
    void register_duplicate_email_returns_error() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "Bob", "lastName", "Jones",
                "email", "bob@test.com", "password", "pass123"));

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Second registration with same email should fail
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is4xxClientError());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login → 200 with JWT token")
    void login_returns_200_and_token() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",    "user@test.com",
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    // ── Admin register ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/admin/register → 201 with valid admin secret")
    void admin_register_with_valid_secret() throws Exception {
        mockMvc.perform(post("/api/auth/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName",   "Admin",
                                "lastName",    "User",
                                "email",       "admin@test.com",
                                "password",    "adminpass",
                                "adminSecret", adminSecret))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/admin/register → 403 with wrong admin secret")
    void admin_register_with_wrong_secret_returns_403() throws Exception {
        mockMvc.perform(post("/api/auth/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName",   "Hacker",
                                "lastName",    "Guy",
                                "email",       "hacker@test.com",
                                "password",    "pass",
                                "adminSecret", "wrong-secret"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/auth/admin/login → 200 with valid admin credentials")
    void admin_login_with_valid_credentials() throws Exception {
        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",       "admin@test.com",
                                "password",    "adminpass",
                                "adminSecret", adminSecret))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }
}

package com.shopverse.web.controller;

import com.shopverse.application.usecase.customer.RegisterCustomerUseCase;
import com.shopverse.domain.model.Customer;
import com.shopverse.infrastructure.redis.RedisSessionStore;
import com.shopverse.security.JwtTokenProvider;
import com.shopverse.shared.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * Ch07-06: Auth endpoints — register + login (returns JWT).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterCustomerUseCase registerUseCase;
    private final JwtTokenProvider        jwtProvider;
    private final RedisSessionStore       redisSessionStore;

    @Value("${shopverse.admin.secret}")
    private String adminSecret;

    public AuthController(RegisterCustomerUseCase registerUseCase,
                          JwtTokenProvider jwtProvider,
                          RedisSessionStore redisSessionStore) {
        this.registerUseCase   = registerUseCase;
        this.jwtProvider       = jwtProvider;
        this.redisSessionStore = redisSessionStore;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        Customer customer = registerUseCase.execute(req.firstName(), req.lastName(), req.email());
        String token = jwtProvider.generateToken(req.email(), "USER");
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(new AuthResponse(token, customer.getId(), customer.getEmail(),
                        customer.getFullName(), "USER")));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerAdmin(@Valid @RequestBody AdminRegisterRequest req) {
        if (!adminSecret.equals(req.adminSecret())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("FORBIDDEN", "Invalid admin secret"));
        }
        Customer customer = registerUseCase.execute(req.firstName(), req.lastName(), req.email());
        String token = jwtProvider.generateToken(req.email(), "ADMIN");
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(new AuthResponse(token, customer.getId(), customer.getEmail(),
                        customer.getFullName(), "ADMIN")));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        // Simplified: in production verify password hash
        String token = jwtProvider.generateToken(req.email(), "USER");
        return ResponseEntity.ok(ApiResponse.ok(
                new AuthResponse(token, 1L, req.email(), "Demo User", "USER"),
                "Login successful"));
    }

    /**
     * POST /api/auth/logout
     * Adds the JWT to a Redis blocklist so it is rejected by JwtAuthenticationFilter
     * even though its cryptographic signature is still valid.
     * The Redis key auto-expires when the JWT itself would have expired, keeping
     * memory bounded with no extra cleanup job needed.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtProvider.isValid(token)) {
                long remainingMs = jwtProvider.getRemainingTtlMs(token);
                if (remainingMs > 0) {
                    redisSessionStore.blocklist(token, Duration.ofMillis(remainingMs));
                }
            }
        }
        // Always return 200 — even if no token was sent, the client-side session is cleared
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdmin(@Valid @RequestBody AdminLoginRequest req) {
        if (!adminSecret.equals(req.adminSecret())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("FORBIDDEN", "Invalid admin secret"));
        }
        String token = jwtProvider.generateToken(req.email(), "ADMIN");
        return ResponseEntity.ok(ApiResponse.ok(
                new AuthResponse(token, null, req.email(), "Admin User", "ADMIN"),
                "Admin login successful"));
    }

    public record RegisterRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Email @NotBlank String email,
            @NotBlank String password) {}

    public record AdminRegisterRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String adminSecret) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password) {}

    public record AdminLoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String adminSecret) {}

    public record AuthResponse(
            String token, Long customerId, String email, String name, String role) {}
}

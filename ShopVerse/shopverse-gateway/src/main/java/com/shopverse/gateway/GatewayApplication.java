package com.shopverse.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ch14-06: Spring Cloud Gateway — edge proxy for ShopVerse.
 * Routes: /api/** → shopverse-web, rate limiting, circuit breaking.
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}

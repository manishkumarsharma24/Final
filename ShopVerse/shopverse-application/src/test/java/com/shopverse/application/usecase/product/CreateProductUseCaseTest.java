package com.shopverse.application.usecase.product;

import com.shopverse.application.command.UpdateProductCommand;
import com.shopverse.application.service.cache.CachedProductService;
import com.shopverse.domain.model.Product;
import com.shopverse.domain.port.EventPublisher;
import com.shopverse.domain.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductUseCase")
class CreateProductUseCaseTest {

    @Mock private CachedProductService cachedProductService;
    @Mock private EventPublisher        eventPublisher;

    @InjectMocks private CreateProductUseCase useCase;

    @Test
    @DisplayName("creates product and publishes event")
    void creates_product_and_publishes_event() {
        UpdateProductCommand cmd = new UpdateProductCommand(
                null, "Widget Pro", "Best widget", new BigDecimal("49.99"),
                "USD", "Electronics", 200);

        Product saved = Product.builder()
                .id(5L).name("Widget Pro")
                .price(new Money(new BigDecimal("49.99"), "USD"))
                .category("Electronics").stockQuantity(200).build();

        when(cachedProductService.save(any())).thenReturn(saved);

        Product result = useCase.execute(cmd);

        assertEquals(5L, result.getId());
        assertEquals("Widget Pro", result.getName());
        verify(cachedProductService).save(any());
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("passes product data from command to service correctly")
    void maps_command_to_product() {
        UpdateProductCommand cmd = new UpdateProductCommand(
                null, "Gadget", "Cool gadget", new BigDecimal("9.99"),
                "USD", "Accessories", 50);

        when(cachedProductService.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            // Verify the product was built correctly from the command
            assertEquals("Gadget", p.getName());
            assertEquals(new BigDecimal("9.99"), p.getPrice().amount());
            assertEquals(50, p.getStockQuantity());
            return Product.builder().id(1L).name(p.getName())
                    .price(p.getPrice()).stockQuantity(p.getStockQuantity()).build();
        });

        assertDoesNotThrow(() -> useCase.execute(cmd));
    }
}

package com.shopverse.application.usecase.order;

import com.shopverse.application.command.PlaceOrderCommand;
import com.shopverse.application.service.discount.TierDiscountStrategy;
import com.shopverse.domain.exception.CustomerNotFoundException;
import com.shopverse.domain.exception.ProductNotFoundException;
import com.shopverse.domain.model.*;
import com.shopverse.domain.port.CustomerRepository;
import com.shopverse.domain.port.EventPublisher;
import com.shopverse.domain.port.OrderActivityRepository;
import com.shopverse.domain.port.OrderRepository;
import com.shopverse.domain.port.ProductRepository;
import com.shopverse.domain.vo.Address;
import com.shopverse.domain.vo.Money;
import com.shopverse.domain.event.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Ch03-01: Application use-case — orchestrates domain objects.
 * Ch04-05: @Transactional wraps the full order placement.
 * Ch03-06: Dependency injection via constructor.
 * Ch06-03: Logs ORDER_PLACED event to Cassandra after commit (outside TX).
 */
@Service
public class PlaceOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlaceOrderUseCase.class);

    private final OrderRepository         orderRepository;
    private final ProductRepository       productRepository;
    private final CustomerRepository      customerRepository;
    private final EventPublisher          eventPublisher;
    private final TierDiscountStrategy    discountStrategy;
    private final OrderActivityRepository orderActivityRepository;

    public PlaceOrderUseCase(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            EventPublisher eventPublisher,
            TierDiscountStrategy discountStrategy,
            OrderActivityRepository orderActivityRepository) {
        this.orderRepository         = orderRepository;
        this.productRepository       = productRepository;
        this.customerRepository      = customerRepository;
        this.eventPublisher          = eventPublisher;
        this.discountStrategy        = discountStrategy;
        this.orderActivityRepository = orderActivityRepository;
    }

    @Transactional
    public Order execute(PlaceOrderCommand cmd) {
        Customer customer = customerRepository.findById(cmd.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(cmd.customerId()));

        Address address = new Address(
                cmd.street(), cmd.city(), cmd.state(), cmd.postalCode(), cmd.country());
        Order order = new Order(null, customer.getId(), address);

        for (PlaceOrderCommand.ItemRequest item : cmd.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));

            Money discountedPrice = discountStrategy.apply(product.getPrice(), customer);
            product.reduceStock(item.quantity());
            productRepository.save(product);

            order.addItem(new OrderItem(product.getId(), product.getName(),
                                        item.quantity(), discountedPrice));
        }

        order.confirm();
        Order saved = orderRepository.save(order);

        // Ch14-01: Publish domain event
        eventPublisher.publish(new OrderEvent.OrderPlaced(
                saved.getId(), customer.getId(),
                saved.total().amount(), Instant.now()));

        // Loyalty points: 1 point per dollar
        customer.addLoyaltyPoints(saved.total().amount().intValue());
        customerRepository.save(customer);

        // Ch06-03: Log activity to Cassandra — outside @Transactional boundary.
        // Wrapped in try-catch: a Cassandra failure must never roll back the PostgreSQL order.
        try {
            orderActivityRepository.save(new OrderActivity(
                    customer.getId(),
                    saved.getId(),
                    "ORDER_PLACED",
                    String.format("Order #%d placed — total $%.2f, %d item(s)",
                            saved.getId(), saved.total().amount(), saved.getItems().size())
            ));
        } catch (Exception ex) {
            log.warn("Failed to log ORDER_PLACED activity to Cassandra for orderId={}: {}",
                    saved.getId(), ex.getMessage());
        }

        return saved;
    }
}

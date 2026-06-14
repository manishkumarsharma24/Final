package com.shopverse.infrastructure.adapter;

import com.shopverse.domain.exception.OrderNotFoundException;
import com.shopverse.domain.model.*;
import com.shopverse.domain.port.OrderRepository;
import com.shopverse.domain.vo.Address;
import com.shopverse.domain.vo.Money;
import com.shopverse.infrastructure.jpa.entity.*;
import com.shopverse.infrastructure.jpa.repository.JpaOrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Ch03-04: Hexagonal adapter — Order domain port → JPA. */
@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final JpaOrderRepository jpaRepo;

    public JpaOrderRepositoryAdapter(JpaOrderRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity;
        if (order.getId() != null) {
            // UPDATE path: load the managed entity so Hibernate keeps the @Version value.
            // Reconstructing a detached entity from scratch loses the version field and causes
            // "uninitialized version value 'null'" on save.
            entity = jpaRepo.findById(order.getId())
                    .orElseThrow(() -> new OrderNotFoundException(order.getId()));
            applyDomainChanges(entity, order);
        } else {
            // INSERT path: build a fresh entity (no id, no version yet).
            entity = toEntity(order);
        }
        OrderEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(Long customerId) {
        return jpaRepo.findByCustomerIdOrderByCreatedAtDesc(customerId)
                      .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return jpaRepo.findByStatus(status.name())
                      .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findAll() {
        return jpaRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) { jpaRepo.deleteById(id); }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    /** INSERT only — builds a brand-new entity with no id / version. */
    private OrderEntity toEntity(Order o) {
        OrderEntity e = new OrderEntity();
        e.setCustomerId(o.getCustomerId());
        e.setStatus(o.getStatus().name());
        e.setTrackingNumber(o.getTrackingNumber());
        if (o.getShippingAddress() != null) {
            e.setShippingAddress(toAddressEmbeddable(o.getShippingAddress()));
        }
        // Use addItem() to set both sides of the bidirectional relationship.
        // This ensures ie.order = e before Hibernate flushes, so order_id is non-null on INSERT.
        o.getItems().forEach(i -> {
            OrderItemEntity ie = new OrderItemEntity();
            ie.setProductId(i.getProductId());
            ie.setProductName(i.getProductName());
            ie.setQuantity(i.getQuantity());
            ie.setUnitPrice(i.getUnitPrice().amount());
            ie.setCurrency(i.getUnitPrice().currency());
            e.addItem(ie);
        });
        return e;
    }

    /**
     * UPDATE only — mutates the already-managed entity with the domain object's
     * changed fields (status, trackingNumber).  Items are immutable after placement
     * so we never touch the items collection here.
     */
    private void applyDomainChanges(OrderEntity e, Order o) {
        e.setStatus(o.getStatus().name());
        e.setTrackingNumber(o.getTrackingNumber());
        // shipping address and items don't change after order placement
    }

    private AddressEmbeddable toAddressEmbeddable(Address a) {
        AddressEmbeddable addr = new AddressEmbeddable();
        addr.setStreet(a.street());
        addr.setCity(a.city());
        addr.setState(a.state());
        addr.setPostalCode(a.postalCode());
        addr.setCountry(a.country());
        return addr;
    }

    private Order toDomain(OrderEntity e) {
        AddressEmbeddable a = e.getShippingAddress();
        Address addr = a != null
            ? new Address(a.getStreet(), a.getCity(), a.getState(), a.getPostalCode(), a.getCountry())
            : new Address("N/A", "N/A", null, "00000", "US");
        Order o = new Order(e.getId(), e.getCustomerId(), addr);
        e.getItems().forEach(i -> o.addItem(new OrderItem(
            i.getProductId(), i.getProductName(), i.getQuantity(),
            new Money(i.getUnitPrice(), i.getCurrency()))));
        // Replay status transitions from PENDING to the stored status
        OrderStatus target = OrderStatus.valueOf(e.getStatus());
        if (target != OrderStatus.PENDING) applyStatus(o, target, e.getTrackingNumber());
        return o;
    }

    private void applyStatus(Order o, OrderStatus target, String trackingNumber) {
        try {
            switch (target) {
                case CONFIRMED   -> o.confirm();
                case PROCESSING  -> { o.confirm(); o.startProcessing(); }
                case SHIPPED     -> { o.confirm(); o.startProcessing();
                                     o.ship(trackingNumber != null ? trackingNumber : ""); }
                case DELIVERED   -> { o.confirm(); o.startProcessing();
                                     o.ship(trackingNumber != null ? trackingNumber : "");
                                     o.deliver(); }
                case CANCELLED   -> o.cancel();
                default -> {}
            }
        } catch (Exception ignored) {}
    }
}

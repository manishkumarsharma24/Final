package com.shopverse.infrastructure.jpa.repository;

import com.shopverse.infrastructure.jpa.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/** Ch04-04: Order JPA repository. */
public interface JpaOrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<OrderEntity> findByStatus(String status);

    // Ch05-06: Window function via native query — running total per customer
    @Query(value = """
        SELECT o.id, o.customer_id, o.status,
               SUM(oi.quantity * oi.unit_price) OVER (PARTITION BY o.customer_id ORDER BY o.created_at)
                   AS running_total
        FROM orders o
        JOIN order_items oi ON oi.order_id = o.id
        WHERE o.customer_id = :customerId
        ORDER BY o.created_at
        """, nativeQuery = true)
    List<Object[]> findOrdersWithRunningTotal(@Param("customerId") Long customerId);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status AND o.createdAt >= :since")
    long countByStatusSince(@Param("status") String status, @Param("since") Instant since);
}

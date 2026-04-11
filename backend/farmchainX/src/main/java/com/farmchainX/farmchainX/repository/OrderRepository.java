package com.farmchainX.farmchainX.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.farmchainX.farmchainX.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByRetailerIdOrderByCreatedAtDesc(Long retailerId);

    List<Order> findByRetailerIdOrderByCreatedAtDesc(Long retailerId, Pageable pageable);

    long countByRetailerIdAndStatusNot(Long retailerId, String status);
}

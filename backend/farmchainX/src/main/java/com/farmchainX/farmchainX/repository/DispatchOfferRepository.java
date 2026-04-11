package com.farmchainX.farmchainX.repository;

import com.farmchainX.farmchainX.model.DispatchOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispatchOfferRepository extends JpaRepository<DispatchOffer, Long> {

    // Find all offers with a specific status, ordered by creation date descending
    List<DispatchOffer> findByStatusOrderByCreatedAtDesc(String status);

    // Find a specific offer for a product with a specific status
    Optional<DispatchOffer> findByProductIdAndStatus(Long productId, String status);

    // Find all offers created by a specific distributor, ordered by creation date
    // descending
    List<DispatchOffer> findByDistributorIdOrderByCreatedAtDesc(Long distributorId);

    // Check if a pending offer exists for a product
    boolean existsByProductIdAndStatus(Long productId, String status);
}

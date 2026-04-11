package com.farmchainX.farmchainX.repository;

import com.farmchainX.farmchainX.model.RetailerInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetailerInventoryRepository extends JpaRepository<RetailerInventory, Long> {

    // Find all inventory items for a specific retailer
    List<RetailerInventory> findByRetailerId(Long retailerId);

    // Find inventory items by retailer and status
    List<RetailerInventory> findByRetailerIdAndStatus(Long retailerId, String status);

    // Find all inventory records for a specific product (across all retailers)
    List<RetailerInventory> findByProductId(Long productId);

    // Find a specific retailer's inventory record for a product
    Optional<RetailerInventory> findByRetailerIdAndProductId(Long retailerId, Long productId);

    // Check if a retailer already has this product in inventory
    boolean existsByRetailerIdAndProductId(Long retailerId, Long productId);

    // Find all IN_STOCK items across all retailers (for consumer marketplace)
    List<RetailerInventory> findByStatus(String status);

    // Find all inventory items from a specific distributor
    List<RetailerInventory> findBySourceDistributorId(Long distributorId);
}

package com.farmchainX.farmchainX.repository;

import com.farmchainX.farmchainX.model.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByFarmerId(Long farmerId);

    Optional<Product> findByPublicUuid(String uuid);

    Page<Product> findByFarmerId(Long farmerId, Pageable pageable);
    
    @Query("SELECT p FROM Product p " +
           "WHERE (:cropName IS NULL OR p.cropName = :cropName) " +
           "AND (:endDate IS NULL OR p.harvestDate <= :endDate)")
    List<Product> filterProducts(
            @Param("cropName") String cropName,
            @Param("endDate") LocalDate endDate
    );
}
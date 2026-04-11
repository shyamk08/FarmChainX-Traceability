package com.farmchainX.farmchainX.repository;

import com.farmchainX.farmchainX.model.AIPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIPredictionRepository extends JpaRepository<AIPrediction, Long> {
    
    List<AIPrediction> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    
    List<AIPrediction> findByProductIdOrderByCreatedAtDesc(Long productId);
    
    @Query("SELECT p FROM AIPrediction p WHERE p.farmer.id = :farmerId ORDER BY p.createdAt DESC")
    List<AIPrediction> findAllByFarmerId(@Param("farmerId") Long farmerId);
}


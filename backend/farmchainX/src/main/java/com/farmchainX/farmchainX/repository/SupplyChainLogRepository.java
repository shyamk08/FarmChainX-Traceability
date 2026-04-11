package com.farmchainX.farmchainX.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import com.farmchainX.farmchainX.model.SupplyChainLog;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface SupplyChainLogRepository extends JpaRepository<SupplyChainLog, Long> {

  List<SupplyChainLog> findByProductIdOrderByTimestampAsc(Long productId);

  boolean existsByProductId(Long productId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<SupplyChainLog> findTopByProductIdOrderByTimestampDesc(Long productId);

  // Non-locking version for read-only checks
  Optional<SupplyChainLog> findFirstByProductIdOrderByTimestampDesc(Long productId);

  @Query("""
      SELECT log FROM SupplyChainLog log
      WHERE log.toUserId = :retailerId
        AND log.confirmed = false
        AND log.rejected = false
        AND log.id = (
              SELECT MAX(l2.id)
              FROM SupplyChainLog l2
              WHERE l2.productId = log.productId
        )
      """)
  Page<SupplyChainLog> findPendingForRetailer(@Param("retailerId") Long retailerId, Pageable pageable);

  @Query("""
      SELECT COUNT(log) FROM SupplyChainLog log
      WHERE log.toUserId = :retailerId
        AND log.confirmed = false
        AND log.rejected = false
        AND log.id = (
              SELECT MAX(l2.id)
              FROM SupplyChainLog l2
              WHERE l2.productId = log.productId
        )
      """)
  long countPendingForRetailer(@Param("retailerId") Long retailerId);

  @Query("SELECT COUNT(l) FROM SupplyChainLog l WHERE l.toUserId = :userId AND l.confirmed = true")
  long countConfirmedByToUserId(@Param("userId") Long userId);

  List<SupplyChainLog> findByToUserIdAndConfirmedTrue(Long toUserId);

  List<SupplyChainLog> findByToUserIdOrderByTimestampDesc(Long toUserId);

  @Query("""
      SELECT COUNT(l) FROM SupplyChainLog l
      WHERE l.fromUserId = :distributorId
        AND l.toUserId IS NOT NULL
        AND l.toUserId != :distributorId
        AND l.confirmed = false
        AND l.id = (
            SELECT MAX(l2.id)
            FROM SupplyChainLog l2
            WHERE l2.productId = l.productId
        )
      """)
  long countPendingHandover(@Param("distributorId") Long distributorId);

  List<SupplyChainLog> findByFromUserId(Long fromUserId);

  boolean existsByFromUserIdAndProductId(Long fromUserId, Long productId);

  List<SupplyChainLog> findByToUserId(Long toUserId);
}
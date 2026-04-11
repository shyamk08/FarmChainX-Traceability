package com.farmchainX.farmchainX.repository;

import com.farmchainX.farmchainX.model.AdminPromotionRequest;
import com.farmchainX.farmchainX.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminPromotionRequestRepository extends JpaRepository<AdminPromotionRequest, Long> {

    List<AdminPromotionRequest> findByApprovedFalseAndRejectedFalse();

    boolean existsByUserAndApprovedFalseAndRejectedFalse(User user);

    AdminPromotionRequest findByUserAndApprovedFalseAndRejectedFalse(User user);
}
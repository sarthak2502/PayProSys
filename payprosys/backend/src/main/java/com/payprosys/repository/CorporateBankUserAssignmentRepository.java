package com.payprosys.repository;

import com.payprosys.entity.CorporateBankUserAssignment;
import com.payprosys.entity.CorporateBankUserAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CorporateBankUserAssignmentRepository extends JpaRepository<CorporateBankUserAssignment, CorporateBankUserAssignmentId> {

    @Query("SELECT a FROM CorporateBankUserAssignment a JOIN FETCH a.user u LEFT JOIN FETCH u.roles WHERE a.corporateId = :corporateId ORDER BY a.userId")
    List<CorporateBankUserAssignment> findByCorporateIdWithUserAndRoles(@Param("corporateId") UUID corporateId);

    default List<CorporateBankUserAssignment> findByCorporateIdOrderByUserId(UUID corporateId) {
        return findByCorporateIdWithUserAndRoles(corporateId);
    }

    boolean existsByCorporateIdAndUserId(UUID corporateId, UUID userId);

    void deleteByCorporateIdAndUserId(UUID corporateId, UUID userId);
}

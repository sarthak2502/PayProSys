package com.payprosys.repository;

import com.payprosys.entity.PayrollBatchReviewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PayrollBatchReviewEventRepository extends JpaRepository<PayrollBatchReviewEvent, UUID> {

    List<PayrollBatchReviewEvent> findByPayrollBatch_IdOrderByCreatedAtAsc(UUID batchId);

    @Query("SELECT e FROM PayrollBatchReviewEvent e JOIN FETCH e.actor WHERE e.payrollBatch.id = :batchId ORDER BY e.createdAt ASC")
    List<PayrollBatchReviewEvent> findByPayrollBatch_IdWithActorOrderByCreatedAtAsc(@Param("batchId") UUID batchId);

    long countByPayrollBatch_Id(UUID batchId);
}

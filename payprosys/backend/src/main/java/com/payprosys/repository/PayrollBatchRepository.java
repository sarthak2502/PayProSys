package com.payprosys.repository;

import com.payprosys.entity.PayrollBatch;
import com.payprosys.entity.PayrollBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollBatchRepository extends JpaRepository<PayrollBatch, UUID> {

    List<PayrollBatch> findByCorporateIdOrderByCreatedAtDesc(UUID corporateId);

    List<PayrollBatch> findByCorporateIdAndBatchStatusOrderByCreatedAtDesc(UUID corporateId, PayrollBatchStatus batchStatus);

    List<PayrollBatch> findByCorporateIdAndYearMonthOrderByCreatedAtDesc(UUID corporateId, Integer yearMonth);

    @Query("SELECT b FROM PayrollBatch b JOIN FETCH b.corporate c JOIN FETCH c.bank JOIN FETCH b.uploadedBy WHERE c.bank.id = :bankId AND b.batchStatus = :status ORDER BY b.createdAt DESC")
    List<PayrollBatch> findByBankIdAndBatchStatusOrderByCreatedAtDesc(@Param("bankId") UUID bankId, @Param("status") PayrollBatchStatus status);

    @Query("SELECT b FROM PayrollBatch b JOIN FETCH b.corporate c JOIN FETCH b.uploadedBy WHERE b.id = :id")
    Optional<PayrollBatch> findByIdWithCorporateAndUploadedBy(@Param("id") UUID id);
}

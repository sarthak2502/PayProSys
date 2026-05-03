package com.payprosys.repository;

import com.payprosys.entity.BankFlowState;
import com.payprosys.entity.CorporateFlowState;
import com.payprosys.entity.PayrollBatch;
import com.payprosys.entity.PayrollBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollBatchRepository extends JpaRepository<PayrollBatch, UUID> {

    List<PayrollBatch> findByCorporateIdOrderByCreatedAtDesc(UUID corporateId);

    List<PayrollBatch> findByCorporateIdAndBatchStatusOrderByCreatedAtDesc(UUID corporateId, PayrollBatchStatus batchStatus);

    List<PayrollBatch> findByCorporateIdAndBatchStatusInOrderByCreatedAtDesc(UUID corporateId, Collection<PayrollBatchStatus> statuses);

    List<PayrollBatch> findByCorporateIdAndYearMonthOrderByCreatedAtDesc(UUID corporateId, Integer yearMonth);

    @Query("SELECT b FROM PayrollBatch b JOIN FETCH b.corporate c JOIN FETCH c.bank JOIN FETCH b.uploadedBy WHERE c.bank.id = :bankId AND b.batchStatus = :status ORDER BY b.createdAt DESC")
    List<PayrollBatch> findByBankIdAndBatchStatusOrderByCreatedAtDesc(@Param("bankId") UUID bankId, @Param("status") PayrollBatchStatus status);

    @Query("SELECT b FROM PayrollBatch b JOIN FETCH b.corporate c JOIN FETCH c.bank JOIN FETCH b.uploadedBy WHERE c.bank.id = :bankId AND ("
            + "(b.batchStatus = :submitted AND b.corporateFlowState = :corpSentToBank) OR "
            + "(b.batchStatus = :pending AND b.corporateFlowState = :corpClarification AND b.bankFlowState = :bankRecall)"
            + ") ORDER BY b.createdAt DESC")
    List<PayrollBatch> findForBankInbox(
            @Param("bankId") UUID bankId,
            @Param("submitted") PayrollBatchStatus submitted,
            @Param("corpSentToBank") CorporateFlowState corpSentToBank,
            @Param("pending") PayrollBatchStatus pending,
            @Param("corpClarification") CorporateFlowState corpClarification,
            @Param("bankRecall") BankFlowState bankRecall);

    @Query("SELECT b FROM PayrollBatch b JOIN FETCH b.corporate c LEFT JOIN FETCH c.bank JOIN FETCH b.uploadedBy WHERE b.id = :id")
    Optional<PayrollBatch> findByIdWithCorporateAndUploadedBy(@Param("id") UUID id);

    @Query("SELECT b FROM PayrollBatch b JOIN FETCH b.corporate c JOIN FETCH c.bank JOIN FETCH b.uploadedBy WHERE c.bank.id = :bankId AND b.bankFlowState = :state ORDER BY b.createdAt DESC")
    List<PayrollBatch> findByBankIdAndBankFlowStateOrderByCreatedAtDesc(@Param("bankId") UUID bankId, @Param("state") BankFlowState state);
}

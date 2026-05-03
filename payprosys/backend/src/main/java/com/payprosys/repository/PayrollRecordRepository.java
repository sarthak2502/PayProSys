package com.payprosys.repository;

import com.payprosys.entity.PayrollBatchStatus;
import com.payprosys.entity.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, UUID> {

    @Query("SELECT r FROM PayrollRecord r JOIN FETCH r.payrollBatch b WHERE b.corporate.id = :corporateId AND b.batchStatus = :batchStatus ORDER BY b.yearMonth DESC, b.createdAt DESC, r.employeeName")
    List<PayrollRecord> findByCorporateIdAndBatchStatusOrderByMonthAndName(@Param("corporateId") UUID corporateId, @Param("batchStatus") PayrollBatchStatus batchStatus);

    @Query("SELECT r FROM PayrollRecord r JOIN FETCH r.payrollBatch b WHERE b.corporate.id = :corporateId AND b.yearMonth = :yearMonth AND b.batchStatus = :batchStatus ORDER BY b.createdAt DESC, r.employeeName")
    List<PayrollRecord> findByCorporateIdAndYearMonthAndBatchStatusOrder(@Param("corporateId") UUID corporateId, @Param("yearMonth") Integer yearMonth, @Param("batchStatus") PayrollBatchStatus batchStatus);

    @Query("SELECT r FROM PayrollRecord r JOIN FETCH r.payrollBatch b JOIN FETCH b.corporate c WHERE b.id = :batchId ORDER BY r.employeeName")
    List<PayrollRecord> findByBatchIdWithFetch(@Param("batchId") UUID batchId);
}

package com.payprosys.repository;

import com.payprosys.entity.Corporate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CorporateRepository extends JpaRepository<Corporate, UUID> {

    List<Corporate> findByBankIdOrderByCreatedAtDesc(UUID bankId);
}

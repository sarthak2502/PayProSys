package com.payprosys.repository;

import com.payprosys.entity.RoleName;
import com.payprosys.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles LEFT JOIN FETCH u.bank LEFT JOIN FETCH u.corporate WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    Optional<User> findByActivationToken(String activationToken);

    boolean existsByEmail(String email);

    List<User> findByBankId(UUID bankId);

    List<User> findByCorporateId(UUID corporateId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.bank.id = :bankId")
    List<User> findByBankIdWithRoles(@Param("bankId") UUID bankId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.corporate.id = :corporateId")
    List<User> findByCorporateIdWithRoles(@Param("corporateId") UUID corporateId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles LEFT JOIN FETCH u.bank LEFT JOIN FETCH u.corporate ORDER BY u.email")
    List<User> findAllWithRolesBankCorporate();
}

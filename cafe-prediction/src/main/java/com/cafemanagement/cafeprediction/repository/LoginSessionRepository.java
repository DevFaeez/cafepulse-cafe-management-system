package com.cafemanagement.cafeprediction.repository;

import com.cafemanagement.cafeprediction.model.LoginSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface LoginSessionRepository extends JpaRepository<LoginSession, String> {
    @EntityGraph(attributePaths = "account")
    Optional<LoginSession> findByIdAndExpiresAtAfter(String id, Instant now);

    @Modifying
    @Query("delete from LoginSession s where s.account.id = :id")
    void revokeAccount(@Param("id") Long id);

    void deleteByAccountIdAndExpiresAtBefore(Long accountId, Instant now);
}

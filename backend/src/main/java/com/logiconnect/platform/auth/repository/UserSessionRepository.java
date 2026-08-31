package com.logiconnect.platform.auth.repository;

import com.logiconnect.platform.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    List<UserSession> findAllByUser_IdAndRevokedAtIsNull(UUID userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.revokedAt = :revokedAt WHERE s.user.id = :userId AND s.revokedAt IS NULL")
    int revokeAllActiveSessionsForUser(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}

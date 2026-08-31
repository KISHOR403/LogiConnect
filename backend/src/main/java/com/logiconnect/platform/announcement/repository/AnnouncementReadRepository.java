package com.logiconnect.platform.announcement.repository;

import com.logiconnect.platform.announcement.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, UUID> {

    @Query("SELECT r FROM AnnouncementRead r WHERE r.announcement.id = :announcementId AND r.user.id = :userId")
    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(@Param("announcementId") UUID announcementId, @Param("userId") UUID userId);

    @Query("SELECT r FROM AnnouncementRead r LEFT JOIN FETCH r.user u LEFT JOIN FETCH u.employee WHERE r.announcement.id = :announcementId")
    List<AnnouncementRead> findByAnnouncementIdWithUserDetails(@Param("announcementId") UUID announcementId);

    @Query("SELECT r FROM AnnouncementRead r WHERE r.announcement.id IN :announcementIds AND r.user.id = :userId")
    List<AnnouncementRead> findByAnnouncementIdsAndUserId(@Param("announcementIds") Collection<UUID> announcementIds, @Param("userId") UUID userId);

    long countByAnnouncementIdAndReadAtIsNotNull(UUID announcementId);

    long countByAnnouncementIdAndAcknowledgedAtIsNotNull(UUID announcementId);

    boolean existsByAnnouncementIdAndUserId(UUID announcementId, UUID userId);
}

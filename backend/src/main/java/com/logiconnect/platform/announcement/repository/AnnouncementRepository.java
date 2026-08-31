package com.logiconnect.platform.announcement.repository;

import com.logiconnect.platform.announcement.entity.Announcement;
import com.logiconnect.platform.announcement.entity.AnnouncementStatus;
import com.logiconnect.platform.announcement.entity.AnnouncementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    @Query("""
            SELECT a FROM Announcement a
            LEFT JOIN FETCH a.department
            LEFT JOIN FETCH a.team
            LEFT JOIN FETCH a.createdBy cb
            LEFT JOIN FETCH cb.employee
            LEFT JOIN FETCH a.publishedBy pb
            LEFT JOIN FETCH pb.employee
            WHERE a.id = :id
            """)
    Optional<Announcement> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Finds active published announcements visible to an employee based on company, department, and team targeting.
     */
    @Query("""
            SELECT a FROM Announcement a
            LEFT JOIN FETCH a.department
            LEFT JOIN FETCH a.team
            LEFT JOIN FETCH a.createdBy cb
            LEFT JOIN FETCH cb.employee
            LEFT JOIN FETCH a.publishedBy pb
            LEFT JOIN FETCH pb.employee
            WHERE a.status = 'PUBLISHED'
              AND (a.publishedAt IS NOT NULL AND a.publishedAt <= :now)
              AND (a.expiresAt IS NULL OR a.expiresAt > :now)
              AND (
                    a.targetType = com.logiconnect.platform.announcement.entity.AnnouncementTargetType.COMPANY
                    OR (:departmentId IS NOT NULL AND a.targetType = com.logiconnect.platform.announcement.entity.AnnouncementTargetType.DEPARTMENT AND a.department.id = :departmentId)
                    OR (:teamId IS NOT NULL AND a.targetType = com.logiconnect.platform.announcement.entity.AnnouncementTargetType.TEAM AND a.team.id = :teamId)
                  )
              AND (:type IS NULL OR a.type = :type)
              AND (:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.content) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Announcement> findFeedForEmployee(
            @Param("departmentId") UUID departmentId,
            @Param("teamId") UUID teamId,
            @Param("now") Instant now,
            @Param("type") AnnouncementType type,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Admin/Broad view query to list announcements by status, type, and keyword.
     */
    @Query("""
            SELECT a FROM Announcement a
            LEFT JOIN FETCH a.department
            LEFT JOIN FETCH a.team
            LEFT JOIN FETCH a.createdBy cb
            LEFT JOIN FETCH cb.employee
            LEFT JOIN FETCH a.publishedBy pb
            LEFT JOIN FETCH pb.employee
            WHERE (:status IS NULL OR a.status = :status)
              AND (:type IS NULL OR a.type = :type)
              AND (:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.content) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Announcement> findAllAdmin(
            @Param("status") AnnouncementStatus status,
            @Param("type") AnnouncementType type,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Query to list drafts/announcements created by a specific user.
     */
    @Query("""
            SELECT a FROM Announcement a
            LEFT JOIN FETCH a.department
            LEFT JOIN FETCH a.team
            LEFT JOIN FETCH a.createdBy cb
            LEFT JOIN FETCH cb.employee
            LEFT JOIN FETCH a.publishedBy pb
            LEFT JOIN FETCH pb.employee
            WHERE a.createdBy.id = :creatorId
              AND (:status IS NULL OR a.status = :status)
              AND (:type IS NULL OR a.type = :type)
              AND (:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.content) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Announcement> findByCreator(
            @Param("creatorId") UUID creatorId,
            @Param("status") AnnouncementStatus status,
            @Param("type") AnnouncementType type,
            @Param("search") String search,
            Pageable pageable
    );
}

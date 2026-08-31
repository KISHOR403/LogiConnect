package com.logiconnect.platform.channel.repository;

import com.logiconnect.platform.channel.entity.Channel;
import com.logiconnect.platform.channel.entity.ChannelStatus;
import com.logiconnect.platform.channel.entity.ChannelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    Optional<Channel> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
        SELECT c FROM Channel c
        WHERE c.status = :status
        AND (
            c.type = com.logiconnect.platform.channel.entity.ChannelType.COMPANY
            OR (c.type = com.logiconnect.platform.channel.entity.ChannelType.DEPARTMENT AND c.department.id = :departmentId)
            OR (c.type = com.logiconnect.platform.channel.entity.ChannelType.TEAM AND c.team.id = :teamId)
            OR (c.id IN (SELECT cm.channel.id FROM ChannelMember cm WHERE cm.user.id = :userId))
        )
        AND (:type IS NULL OR c.type = :type)
        AND (
            :search IS NULL
            OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :search, '%'))
        )
    """)
    Page<Channel> findAccessibleChannels(
            @Param("userId") UUID userId,
            @Param("departmentId") UUID departmentId,
            @Param("teamId") UUID teamId,
            @Param("type") ChannelType type,
            @Param("status") ChannelStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        SELECT c FROM Channel c
        WHERE c.status = :status
        AND (:type IS NULL OR c.type = :type)
        AND (
            :search IS NULL
            OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :search, '%'))
        )
    """)
    Page<Channel> findAllAdmin(
            @Param("type") ChannelType type,
            @Param("status") ChannelStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}

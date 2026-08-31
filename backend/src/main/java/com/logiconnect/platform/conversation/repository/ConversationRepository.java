package com.logiconnect.platform.conversation.repository;

import com.logiconnect.platform.conversation.entity.Conversation;
import com.logiconnect.platform.conversation.entity.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
            SELECT c FROM Conversation c
            JOIN c.members m1
            JOIN c.members m2
            WHERE c.type = com.logiconnect.platform.conversation.entity.ConversationType.DIRECT
              AND c.archived = false
              AND m1.user.id = :user1Id
              AND m2.user.id = :user2Id
            """)
    List<Conversation> findDirectConversationsBetween(
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id
    );

    @Query("""
            SELECT DISTINCT c FROM Conversation c
            JOIN c.members m
            WHERE m.user.id = :userId
              AND m.leftAt IS NULL
              AND (:type IS NULL OR c.type = :type)
              AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Conversation> findUserConversations(
            @Param("userId") UUID userId,
            @Param("type") ConversationType type,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT c FROM Conversation c
            LEFT JOIN FETCH c.members m
            LEFT JOIN FETCH m.user u
            LEFT JOIN FETCH u.employee e
            WHERE c.id = :id
            """)
    Optional<Conversation> findByIdWithMembers(@Param("id") UUID id);
}

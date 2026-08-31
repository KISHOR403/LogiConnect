package com.logiconnect.platform.message.repository;

import com.logiconnect.platform.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query(value = """
            SELECT m FROM Message m
            LEFT JOIN FETCH m.sender s
            LEFT JOIN FETCH s.employee e
            WHERE m.conversation.id = :conversationId
            """,
            countQuery = """
            SELECT COUNT(m) FROM Message m
            WHERE m.conversation.id = :conversationId
            """)
    Page<Message> findByConversationId(
            @Param("conversationId") UUID conversationId,
            Pageable pageable
    );

    @Query(value = """
            SELECT m FROM Message m
            LEFT JOIN FETCH m.sender s
            LEFT JOIN FETCH s.employee e
            WHERE m.channel.id = :channelId
            """,
            countQuery = """
            SELECT COUNT(m) FROM Message m
            WHERE m.channel.id = :channelId
            """)
    Page<Message> findByChannelId(
            @Param("channelId") UUID channelId,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM Message m
            LEFT JOIN FETCH m.conversation c
            LEFT JOIN FETCH m.channel ch
            LEFT JOIN FETCH m.sender s
            LEFT JOIN FETCH s.employee e
            LEFT JOIN FETCH m.replyToMessage rm
            WHERE m.id = :id
            """)
    Optional<Message> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
            SELECT m FROM Message m
            LEFT JOIN FETCH m.sender s
            LEFT JOIN FETCH s.employee e
            WHERE m.conversation.id = :conversationId
            ORDER BY m.createdAt DESC, m.id DESC
            LIMIT 1
            """)
    Optional<Message> findLatestMessageByConversationId(@Param("conversationId") UUID conversationId);
}

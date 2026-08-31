package com.logiconnect.platform.conversation.repository;

import com.logiconnect.platform.conversation.entity.ConversationMember;
import com.logiconnect.platform.conversation.entity.ConversationMemberId;
import com.logiconnect.platform.conversation.entity.ConversationMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

    @Query("SELECT cm FROM ConversationMember cm WHERE cm.id.conversationId = :conversationId AND cm.id.userId = :userId")
    Optional<ConversationMember> findByConversationIdAndUserId(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId
    );

    @Query("SELECT cm FROM ConversationMember cm JOIN FETCH cm.user u LEFT JOIN FETCH u.employee e WHERE cm.id.conversationId = :conversationId AND cm.leftAt IS NULL ORDER BY cm.joinedAt ASC")
    List<ConversationMember> findActiveMembersByConversationId(@Param("conversationId") UUID conversationId);

    @Query("SELECT COUNT(cm) FROM ConversationMember cm WHERE cm.id.conversationId = :conversationId AND cm.leftAt IS NULL")
    long countActiveMembersByConversationId(@Param("conversationId") UUID conversationId);

    @Query("SELECT COUNT(cm) FROM ConversationMember cm WHERE cm.id.conversationId = :conversationId AND cm.role = :role AND cm.leftAt IS NULL")
    long countActiveMembersByRole(
            @Param("conversationId") UUID conversationId,
            @Param("role") ConversationMemberRole role
    );

    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN TRUE ELSE FALSE END FROM ConversationMember cm WHERE cm.id.conversationId = :conversationId AND cm.id.userId = :userId AND cm.leftAt IS NULL")
    boolean isUserActiveMember(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId
    );
}

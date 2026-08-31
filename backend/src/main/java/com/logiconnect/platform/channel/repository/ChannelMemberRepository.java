package com.logiconnect.platform.channel.repository;

import com.logiconnect.platform.channel.entity.ChannelMember;
import com.logiconnect.platform.channel.entity.ChannelMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, ChannelMemberId> {

    @Query("SELECT cm FROM ChannelMember cm JOIN FETCH cm.user u LEFT JOIN FETCH u.employee e LEFT JOIN FETCH e.department WHERE cm.channel.id = :channelId")
    List<ChannelMember> findByChannelIdWithDetails(@Param("channelId") UUID channelId);

    @Query("SELECT cm FROM ChannelMember cm WHERE cm.channel.id = :channelId AND cm.user.id = :userId")
    Optional<ChannelMember> findByChannelIdAndUserId(@Param("channelId") UUID channelId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(cm) > 0 FROM ChannelMember cm WHERE cm.channel.id = :channelId AND cm.user.id = :userId")
    boolean existsByChannelIdAndUserId(@Param("channelId") UUID channelId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(cm) FROM ChannelMember cm WHERE cm.channel.id = :channelId")
    long countByChannelId(@Param("channelId") UUID channelId);

    @Modifying
    @Query("DELETE FROM ChannelMember cm WHERE cm.channel.id = :channelId AND cm.user.id = :userId")
    void deleteByChannelIdAndUserId(@Param("channelId") UUID channelId, @Param("userId") UUID userId);
}

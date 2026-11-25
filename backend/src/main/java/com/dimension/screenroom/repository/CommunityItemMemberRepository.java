package com.dimension.screenroom.repository;

import com.dimension.screenroom.domain.CommunityItemMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommunityItemMemberRepository extends JpaRepository<CommunityItemMember, Long> {
    Optional<CommunityItemMember> findByUserIdAndItemId(Long userId, Long itemId);
    int countByItemId(Long itemId);
}

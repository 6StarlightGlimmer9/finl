package com.dimension.screenroom.repository;

import com.dimension.screenroom.domain.CommunityItem;
import com.dimension.screenroom.domain.CommunityItemKind;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommunityItemRepository extends JpaRepository<CommunityItem, Long> {
    List<CommunityItem> findByKindOrderByCreatedAtDesc(CommunityItemKind kind);
}

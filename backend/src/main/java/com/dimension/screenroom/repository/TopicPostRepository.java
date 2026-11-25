package com.dimension.screenroom.repository;

import com.dimension.screenroom.domain.TopicPost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TopicPostRepository extends JpaRepository<TopicPost, Long> {
    List<TopicPost> findByItemIdOrderByCreatedAtAsc(Long itemId);
    int countByItemId(Long itemId);
}

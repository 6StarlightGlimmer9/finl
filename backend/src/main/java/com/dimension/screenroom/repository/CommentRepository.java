package com.dimension.screenroom.repository;

import com.dimension.screenroom.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByAnimeIdOrderByCreatedAtDesc(Long animeId);
    List<Comment> findAllByOrderByCreatedAtDesc();
    List<Comment> findByContentContainingIgnoreCase(String keyword);
}

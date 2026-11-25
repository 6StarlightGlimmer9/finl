package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comment_likes", uniqueConstraints = @UniqueConstraint(name = "uk_user_comment_like", columnNames = {"user_id", "comment_id"}))
public class CommentLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) public User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "comment_id", nullable = false) public Comment comment;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}

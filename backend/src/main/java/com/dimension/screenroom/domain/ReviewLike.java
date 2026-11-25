package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "review_likes", uniqueConstraints = @UniqueConstraint(name = "uk_user_review_like", columnNames = {"user_id", "review_id"}))
public class ReviewLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) public User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "review_id", nullable = false) public Review review;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}

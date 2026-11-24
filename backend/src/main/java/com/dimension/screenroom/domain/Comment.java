package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) public User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "anime_id", nullable = false) public Anime anime;
    @Column(nullable = false, length = 1000) public String content;
    @Column(nullable = false) public Integer likes = 0;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}

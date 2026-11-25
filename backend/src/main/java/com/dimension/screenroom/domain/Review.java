package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reviews")
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "user_id", nullable = false) public User user;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "anime_id", nullable = false) public Anime anime;
    @Column(nullable = false, length = 160) public String title;
    @Column(nullable = false, columnDefinition = "TEXT") public String content;
    @Column(nullable = false) public Integer score;
    @Column(nullable = false) public Integer likes = 0;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}

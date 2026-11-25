package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "topic_posts")
public class TopicPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "item_id", nullable = false) public CommunityItem item;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "user_id", nullable = false) public User user;
    @Column(nullable = false, length = 1000) public String content;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}

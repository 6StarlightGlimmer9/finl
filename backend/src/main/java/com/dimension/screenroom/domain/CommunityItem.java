package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "community_items")
public class CommunityItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "enum('TOPIC','GROUP','EVENT')")
    public CommunityItemKind kind;
    @Column(nullable = false, length = 160) public String title;
    @Column(columnDefinition = "TEXT") public String description;
    public String tag;
    public String color;
    @Column(name = "base_count", nullable = false) public Integer baseCount = 0;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "user_id") public User user;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}

package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "community_item_members", uniqueConstraints = @UniqueConstraint(name = "uk_community_item_member", columnNames = {"user_id", "item_id"}))
public class CommunityItemMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) public User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "item_id", nullable = false) public CommunityItem item;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}

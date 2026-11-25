package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "watchlist", uniqueConstraints = @UniqueConstraint(name = "uk_user_anime_watchlist", columnNames = {"user_id", "anime_id"}))
public class Watchlist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "user_id", nullable = false) public User user;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "anime_id", nullable = false) public Anime anime;
    @Enumerated(EnumType.STRING) @Column(nullable = false, columnDefinition = "enum('WANT','WATCHING','WATCHED')") public WatchStatus status = WatchStatus.WANT;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) public Instant updatedAt = Instant.now();
}

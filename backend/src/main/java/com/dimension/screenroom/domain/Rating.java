package com.dimension.screenroom.domain;

import javax.persistence.*;

@Entity
@Table(name = "ratings", uniqueConstraints = @UniqueConstraint(name = "uk_user_anime_rating", columnNames = {"user_id", "anime_id"}))
public class Rating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) public User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "anime_id", nullable = false) public Anime anime;
    @Column(nullable = false, columnDefinition = "tinyint") public Integer score;
}

package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "anime")
public class Anime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false) public String title;
    @Column(name = "original_title") public String originalTitle;
    @Column(columnDefinition = "TEXT") public String summary;
    @Column(name = "cover_url") public String coverUrl;
    @Column(name = "pv_url") public String pvUrl;
    public String season;
    @Column(name = "created_at", insertable = false, updatable = false) public Instant createdAt;
    @Enumerated(EnumType.STRING) @Column(name = "media_type", nullable = false, columnDefinition = "enum('ANIME','MOVIE')") public MediaType mediaType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, columnDefinition = "enum('AIRING','FINISHED','UPCOMING')") public AnimeStatus status;
    @Column(nullable = false, precision = 3, scale = 1) public BigDecimal score = BigDecimal.ZERO;
    @Column(nullable = false) public Integer scoreCount = 0;
    @Column(name = "sort_order", nullable = false) public Integer sortOrder = 0;
    @ManyToMany
    @JoinTable(name = "anime_genres", joinColumns = @JoinColumn(name = "anime_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"))
    public java.util.Set<Genre> genres = new java.util.HashSet<>();
}

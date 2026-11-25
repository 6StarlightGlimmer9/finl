package com.dimension.screenroom.repository;

import com.dimension.screenroom.domain.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserIdAndAnimeId(Long userId, Long animeId);

    @Query("select r from Rating r join fetch r.anime where r.user.id = :userId order by r.createdAt desc")
    List<Rating> findProfileRatings(@Param("userId") Long userId);

    @Query("select coalesce(avg(r.score), 0.0) from Rating r where r.anime.id = :animeId")
    Double averageScore(@Param("animeId") Long animeId);

    @Query("select count(r) from Rating r where r.anime.id = :animeId")
    long countByAnimeId(@Param("animeId") Long animeId);

    @Query("select r.score, count(r) from Rating r where r.anime.id = :animeId group by r.score order by r.score")
    List<Object[]> distribution(@Param("animeId") Long animeId);
}

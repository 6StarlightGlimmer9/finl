package com.dimension.screenroom.repository;

import com.dimension.screenroom.domain.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<Watchlist> findByUserIdAndAnimeId(Long userId, Long animeId);
    void deleteByUserIdAndAnimeId(Long userId, Long animeId);
}

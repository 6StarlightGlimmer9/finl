package com.dimension.screenroom.repository;

import com.dimension.screenroom.domain.Anime;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnimeRepository extends JpaRepository<Anime, Long> {
	List<Anime> findAllByOrderByScoreDesc();
	@Query("select a from Anime a where lower(a.title) like lower(concat('%', :keyword, '%')) or lower(a.originalTitle) like lower(concat('%', :keyword, '%'))")
	List<Anime> search(@Param("keyword") String keyword);
}

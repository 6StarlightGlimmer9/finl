package com.dimension.screenroom.repository;

import com.dimension.screenroom.domain.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    List<Genre> findByNameIn(List<String> names);
    java.util.Optional<Genre> findByName(String name);
}

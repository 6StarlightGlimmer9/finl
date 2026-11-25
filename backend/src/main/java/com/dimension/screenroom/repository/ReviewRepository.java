package com.dimension.screenroom.repository;

import com.dimension.screenroom.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}

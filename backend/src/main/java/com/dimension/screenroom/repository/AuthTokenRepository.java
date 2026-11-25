package com.dimension.screenroom.repository;

import com.dimension.screenroom.domain.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from AuthToken token where token.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}

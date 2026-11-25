package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_tokens")
public class AuthToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    public String tokenHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    public Instant createdAt;
}

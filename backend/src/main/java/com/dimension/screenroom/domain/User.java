package com.dimension.screenroom.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true) public String username;
    @Column(nullable = false) public String nickname;
    @Column(nullable = false, unique = true) public String email;
    @Column(name = "password", nullable = false) public String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "enum('USER','ADMIN')")
    public Role role = Role.USER;
    @Column(name = "avatar_url") public String avatarUrl;
    public String bio;
    @Column(name = "created_at", insertable = false, updatable = false) public Instant createdAt;
}

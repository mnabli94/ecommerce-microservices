package com.demo.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshToken {
    @Id
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    String clientId;

    @Column(nullable = false)
    Instant expiresAt;

    @Column(nullable = false)
    Instant createdAt;

    boolean revoked = false;
    String replacedBy;
}
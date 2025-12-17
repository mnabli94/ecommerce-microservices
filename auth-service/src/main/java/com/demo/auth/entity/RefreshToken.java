package com.demo.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name="refresh_tokens")
@Data
public class RefreshToken {
    @Id
    String id;
    @Column(nullable=false)
    String subject;
    String clientId;
    @Column(nullable=false)
    Instant expiresAt;
    @Column(nullable=false)
    Instant createdAt;
    boolean revoked = false;
    String replacedBy;
}
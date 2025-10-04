package com.demo.auth.service;

import com.demo.auth.entity.RefreshToken;
import com.demo.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshService {
    private final RefreshTokenRepository repo;
    private final long ttlDays;

    public RefreshService(RefreshTokenRepository repo, @Value("${auth.refresh-ttl-days}") long ttlDays) {
        this.repo = repo;
        this.ttlDays = ttlDays;
    }

    @Transactional
    public RefreshToken issue(String subject, String clientId) {
        var rt = new RefreshToken();
        rt.setId(UUID.randomUUID().toString());
        rt.setSubject(subject);
        rt.setClientId(clientId);
        rt.setCreatedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(ttlDays)));
        return repo.save(rt);
    }

    @Transactional
    public RefreshToken rotate(String oldRefreshTokenId) {
        var old = repo.findById(oldRefreshTokenId)
                .orElseThrow(() -> new IllegalArgumentException("refresh_token invalid"));
        if (old.isRevoked() || old.getExpiresAt().isBefore(Instant.now()))
            throw new IllegalStateException("refresh_token expired or revoked");

        var next = issue(old.getSubject(), old.getClientId());
        old.setRevoked(true);
        old.setReplacedBy(next.getId());
        repo.save(old);
        return next;
    }

    @Transactional
    public void revokeCascade(String refreshTokenId) {
        repo.findById(refreshTokenId).ifPresent(rt -> {
            rt.setRevoked(true);
            repo.save(rt);

        });
    }

    public String getSubject(String rtId){
        return repo.findById(rtId).map(RefreshToken::getSubject).orElseThrow();
    }
}

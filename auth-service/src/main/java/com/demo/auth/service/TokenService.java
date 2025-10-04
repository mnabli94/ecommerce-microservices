package com.demo.auth.service;

import com.demo.auth.security.KeyProvider;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {
    private final KeyProvider keys;
    private final String issuer;
    private final long accessTtl;

    public TokenService(KeyProvider keys, @Value("${auth.issuer}") String issuer, @Value("${auth.access-ttl-seconds}") long accessTtl) {
        this.keys = keys;
        this.issuer = issuer;
        this.accessTtl = accessTtl;
    }

    public String createAccessToken(String subject, List<String> roles, List<String> scopes) throws JOSEException {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer("http://auth-service:8080")
                .subject(subject)
                .audience("order-service")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(accessTtl)))
                .claim("scope", String.join(" ", scopes))
                .claim("roles", roles)
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keys.getRsaKey().getKeyID()).build(),
                claims);

        signedJWT.sign(new RSASSASigner(keys.getRsaKey().toPrivateKey()));
        return signedJWT.serialize();
    }
}

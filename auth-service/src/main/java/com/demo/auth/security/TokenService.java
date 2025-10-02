package com.demo.auth.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {
    private final KeyProvider keys;

    public TokenService(KeyProvider keys) {
        this.keys = keys;
    }

    public String generateAccessToken(String username, List<String> roles) throws JOSEException {
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("http://auth-service:8080")
                .subject(username)
                .audience("order-service")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(5, ChronoUnit.MINUTES)))
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

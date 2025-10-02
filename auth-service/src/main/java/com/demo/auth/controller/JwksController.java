package com.demo.auth.controller;

import com.demo.auth.security.KeyProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {
    private final KeyProvider keys;

    public JwksController(KeyProvider keys) {
        this.keys = keys;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> keys() {
        return keys.getJwkSet().toJSONObject();
    }
}

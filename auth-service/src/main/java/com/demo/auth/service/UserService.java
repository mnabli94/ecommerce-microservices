package com.demo.auth.service;

import com.demo.auth.entity.User;
import com.demo.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public User authenticate(String username, String rawPassword) {
        var user = repo.findByUsername(username).orElseThrow(() -> new BadCredentialsException("invalid"));
        if (!encoder.matches(rawPassword, user.getPasswordHash()))
            throw new BadCredentialsException("invalid");
        return user;
    }

    public List<String> rolesByUsername(String username) {
        return repo.findByUsername(username)
                .map(u -> Arrays.asList(u.getRoles().split(",")))
                .orElse(List.of());
    }

    public List<String> scopesByUsername(String username) {
        return List.of("order:read", "order:write", "product:read");
    }

    @Transactional
    public void add(String username, String password, String role) {
        var u = new User();
        u.setUsername(username);
        u.setPasswordHash(new BCryptPasswordEncoder().encode(password));
        u.setRoles(role);
        repo.save(u);
    }
}

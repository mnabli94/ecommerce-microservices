package com.demo.auth.service;

import com.demo.auth.dto.CreateUserRequest;
import com.demo.auth.entity.User;
import com.demo.auth.exception.UsernameAlreadyExistsException;
import com.demo.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.userRepository = repo;
        this.encoder = encoder;
    }

    public User authenticate(String username, String rawPassword) throws BadCredentialsException {
        if (username == null || username.isBlank() || rawPassword == null) {
            log.error("Authentication failed: invalid credentials format");
            throw new BadCredentialsException("invalid");
        }
        var user = userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("Authentication failed: user not found username={}", username);
            return new BadCredentialsException("invalid");
        });
        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            log.error("Authentication failed: wrong password for username={}", username);
            throw new BadCredentialsException("invalid");
        }
        log.info("User authenticated: username={}", username);
        return user;
    }

    public List<String> rolesByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(u -> Arrays.asList(u.getRoles().split(",")))
                .orElse(List.of());
    }

    public List<String> scopesByUsername(String username) {
        var roles = rolesByUsername(username);
        if (roles.contains("ADMIN")) {
            return List.of("order:read", "order:write", "product:read", "product:write");
        }
        return List.of("order:read", "product:read");
    }

    @Transactional
    public User createUser(CreateUserRequest userRequest) {
        String username = userRequest.username();
        log.info("Creating user: username={}", username);
        if (userRepository.existsByUsername(username)) {
            log.error("User creation failed: username already exists username={}", username);
            throw new UsernameAlreadyExistsException(username);
        }
        var u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(userRequest.password()));
        u.setRoles(userRequest.roles());
        User saved = userRepository.save(u);
        log.info("User created: username={}", username);
        return saved;
    }

    public User findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> {
                    log.error("User not found: username={}", username);
                    return new EntityNotFoundException("user with username %s not found".formatted(username));
                });
    }
}

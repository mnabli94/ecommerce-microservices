package com.demo.auth.service;

import com.demo.auth.dto.CreateUserRequest;
import com.demo.auth.entity.User;
import com.demo.auth.exception.UsernameAlreadyExistsException;
import com.demo.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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
            throw new BadCredentialsException("invalid");
        }
        var user = userRepository.findByUsername(username).orElseThrow(()
                -> new BadCredentialsException("invalid"));
        if (!encoder.matches(rawPassword, user.getPasswordHash()))
            throw new BadCredentialsException("invalid");
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
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }
        var u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(userRequest.password()));
        u.setRoles(userRequest.roles());
        return userRepository.save(u);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new EntityNotFoundException("user with username %s not found".formatted(username)));
    }
}

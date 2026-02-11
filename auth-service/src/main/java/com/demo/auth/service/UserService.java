package com.demo.auth.service;

import com.demo.auth.config.MetricsConfig;
import com.demo.auth.dto.UserRequest;
import com.demo.auth.dto.UserPatchRequest;
import com.demo.auth.dto.UserSession;
import com.demo.auth.entity.User;
import com.demo.auth.entity.UserRole;
import com.demo.auth.exception.UsernameAlreadyExistsException;
import com.demo.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final PasswordValidationService passwordValidationService;
    private final com.demo.auth.config.MetricsConfig.AuthMetrics metrics;

    @Value("${auth.max-failed-login-attempts:5}")
    private int maxFailedLoginAttempts;

    @Value("${auth.lockout-duration-minutes:30}")
    private long lockoutDurationMinutes;

    public UserService(UserRepository repo, PasswordEncoder encoder,
            PasswordValidationService passwordValidationService,
            MetricsConfig.AuthMetrics metrics) {
        this.userRepository = repo;
        this.encoder = encoder;
        this.passwordValidationService = passwordValidationService;
        this.metrics = metrics;
    }

    @Transactional
    public User authenticate(String username, String rawPassword) throws BadCredentialsException {
        if (username == null || username.isBlank() || rawPassword == null) {
            log.error("Authentication failed: invalid credentials format");
            throw new BadCredentialsException("Invalid credentials");
        }

        var user = userRepository.findByUsernameIgnoreCase(username).orElseThrow(() -> {
            log.warn("Authentication failed: user not found username={}", username);
            throw new BadCredentialsException("Invalid credentials");
        });

        if (user.isLocked()) {
            if (isLockoutExpired(user)) {
                unlockAccount(user);
            } else {
                log.warn("Authentication failed: account locked for username={}", username);
                throw new LockedException("Account is locked due to too many failed login attempts");
            }
        }

        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            handleFailedLogin(user);
            log.warn("Authentication failed: wrong password for username={}", username);
            throw new BadCredentialsException("Invalid credentials");
        }

        resetFailedLoginAttempts(user);

        log.info("User authenticated successfully: username={}", username);
        return user;
    }

    private void handleFailedLogin(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        user.setLastFailedLogin(Instant.now());

        if (user.getFailedLoginAttempts() >= maxFailedLoginAttempts) {
            lockAccount(user);
        }

        userRepository.save(user);
    }

    private void resetFailedLoginAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setLastFailedLogin(null);
        userRepository.save(user);
    }

    private void lockAccount(User user) {
        user.setLocked(true);
        user.setLockedUntil(Instant.now().plus(lockoutDurationMinutes, ChronoUnit.MINUTES));
        userRepository.save(user);
        log.warn("Account locked for user: {} after {} failed attempts",
                user.getUsername(), user.getFailedLoginAttempts());
    }

    private void unlockAccount(User user) {
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLastFailedLogin(null);
        user.setLockedUntil(null);
        userRepository.save(user);
        log.info("Account unlocked for user: {}", user.getUsername());
    }

    private boolean isLockoutExpired(User user) {
        if (user.getLastFailedLogin() == null) {
            return true;
        }
        return user.getLastFailedLogin()
                .plus(lockoutDurationMinutes, ChronoUnit.MINUTES)
                .isBefore(Instant.now());
    }

    @Cacheable(value = "userRoles", key = "#username.toLowerCase()")
    public SortedSet<String> rolesByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(user -> {
                    TreeSet<String> roles = new TreeSet<>();
                    user.getRoles().forEach(role -> roles.add(role.name()));
                    return roles;
                })
                .orElse(new TreeSet<>());
    }

    @Cacheable(value = "userRoles", key = "#username.toLowerCase() + '_scopes'")
    public List<String> scopesByUsername(String username) {
        var roles = rolesByUsername(username);
        return calculateScopes(List.copyOf(roles));
    }

    @Cacheable(value = "userSessions", key = "#username.toLowerCase()")
    public UserSession prepareSession(String username) {
        log.debug("Preparing session for username={}", username);
        var user = findByUsername(username);
        var roles = user.getRoles().stream()
                .map(UserRole::name)
                .toList();
        var scopes = calculateScopes(roles);
        return new UserSession(user.getUsername(), roles, scopes);
    }

    private List<String> calculateScopes(List<String> roles) {
        if (roles.contains("ADMIN")) {
            return List.of("order:read", "order:write", "product:read", "product:write", "user:read", "user:write");
        }
        if (roles.contains("MODERATOR")) {
            return List.of("order:read", "product:read", "user:read");
        }
        return List.of("order:read", "product:read");
    }

    @Transactional
    public User createUser(UserRequest userRequest) {
        String username = userRequest.username();
        log.info("Creating user: username={}", username);

        if (!isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username format");
        }

        if (userRepository.existsByUsername(username)) {
            log.error("User creation failed: username already exists username={}", username);
            throw new UsernameAlreadyExistsException(username);
        }

        passwordValidationService.validatePassword(userRequest.password());

        var user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(userRequest.password()));
        user.setLocked(false);
        user.setFailedLoginAttempts(0);

        for (UserRole role : userRequest.roles()) {
            user.addRole(role);
        }
        if (user.getRoles().isEmpty()) {
            user.addRole(UserRole.USER);
        }

        User saved = userRepository.save(user);
        metrics.getUserCreationCounter().increment();
        log.info("User created successfully: username={} with roles={}",
                username, saved.getRolesAsString());
        return saved;
    }

    @Transactional
    @CacheEvict(value = { "userData", "userSessions", "userRoles" }, key = "#username.toLowerCase()")
    public User updateUser(String username, UserRequest userRequest) {
        log.info("Updating user: username={}", username);

        User user = findByUsername(username);

        if (userRequest.password() != null && !userRequest.password().trim().isEmpty()) {
            passwordValidationService.validatePassword(userRequest.password());
            user.setPasswordHash(encoder.encode(userRequest.password()));
        }

        Set<UserRole> newRoles = userRequest.roles();
        if (newRoles == null || newRoles.isEmpty()) {
            newRoles = Set.of(UserRole.USER);
        }

        // Create a copy of current roles to calculate differences
        Set<UserRole> currentRoles = user.getRoles();

        // Roles to remove: present in current but not in new
        Set<UserRole> toRemove = new HashSet<>(currentRoles);
        toRemove.removeAll(newRoles);
        toRemove.forEach(user::removeRole);

        // Roles to add: present in new but not in current
        Set<UserRole> toAdd = new HashSet<>(newRoles);
        toAdd.removeAll(currentRoles);
        toAdd.forEach(user::addRole);

        User saved = userRepository.save(user);
        log.info("User updated successfully: username={} with roles={}",
                username, saved.getRolesAsString());
        return saved;
    }

    @Transactional
    @CacheEvict(value = { "userData", "userSessions", "userRoles" }, key = "#username.toLowerCase()")
    public void deleteUser(String username) {
        log.info("Deleting user: username={}", username);

        User user = findByUsername(username);
        userRepository.delete(user);

        log.info("User deleted successfully: username={}", username);
    }

    @Transactional
    @CacheEvict(value = { "userData", "userSessions", "userRoles" }, key = "#username.toLowerCase()")
    public void patchUser(String username, UserPatchRequest patch) {
        User user = findByUsername(username);
        boolean modified = false;

        if (patch.locked() != null) {
            user.setLocked(patch.locked());
            if (patch.locked()) {
                user.setLockedUntil(patch.lockedUntil());
                log.info("User {} locked by admin: {}", username, patch.lockReason());
            } else {
                user.setFailedLoginAttempts(0);
                user.setLastFailedLogin(null);
                user.setLockedUntil(null);
                log.info("User {} unlocked by admin: {}", username, patch.lockReason());
            }
            modified = true;
        }

        if (Boolean.TRUE.equals(patch.resetAttempts())) {
            user.setFailedLoginAttempts(0);
            user.setLastFailedLogin(null);
            log.info("Failed login attempts reset for user {}", username);
            modified = true;
        }

        if (modified) {
            userRepository.save(user);
        }
    }

    private boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_-]{3,50}$");
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

package com.demo.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_locked", columnList = "locked")
})
@Data
@EqualsAndHashCode(exclude = "roleRefs")
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    @NotBlank(message = "Username cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$", message = "Username must be 3-50 chars, alphanumeric with _-")
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    @NotBlank(message = "Password hash cannot be blank")
    private String passwordHash;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<UserRoleRef> roleRefs = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 100)
    private String updatedBy;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column
    private Instant lastFailedLogin;

    @Column
    private Instant lockedUntil;

    public Set<UserRole> getRoles() {
        return roleRefs.stream()
                .map(UserRoleRef::getRole)
                .collect(Collectors.toSet());
    }

    public void addRole(UserRole role) {
        UserRoleRef roleRef = new UserRoleRef(this, role);
        roleRefs.add(roleRef);
    }

    public void removeRole(UserRole role) {
        roleRefs.removeIf(ref -> ref.getRole() == role);
    }

    public boolean hasRole(UserRole role) {
        return roleRefs.stream()
                .anyMatch(ref -> ref.getRole() == role);
    }

    public boolean isAdmin() {
        return roleRefs.stream()
                .anyMatch(UserRoleRef::isAdminRole);
    }

    public boolean canModerate() {
        return roleRefs.stream()
                .anyMatch(ref -> ref.getRole() != null && ref.getRole().canModerate());
    }

    public boolean isAccountNonLocked() {
        return !locked || (lockedUntil != null && lockedUntil.isBefore(Instant.now()));
    }

    public String getRolesAsString() {
        return getRoles().stream()
                .map(UserRole::name)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }
}
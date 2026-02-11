package com.demo.auth.service;

import com.demo.auth.config.MetricsConfig;
import com.demo.auth.dto.UserRequest;
import com.demo.auth.dto.UserPatchRequest;
import com.demo.auth.dto.UserSession;
import com.demo.auth.entity.User;
import com.demo.auth.entity.UserRole;
import com.demo.auth.entity.UserRoleRef;
import com.demo.auth.repository.UserRepository;
import com.demo.auth.service.PasswordValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import io.micrometer.core.instrument.Counter;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private User user;
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String HASH = "$hash";

    @Mock
    private PasswordEncoder encoder;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordValidationService passwordValidationService;

    @Mock
    MetricsConfig.AuthMetrics metrics;

    @Mock
    Counter userCreationCounter;

    @InjectMocks
    UserService userService;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername(USERNAME);
        user.setPasswordHash(HASH);
        user.addRole(UserRole.USER);
        user.addRole(UserRole.ADMIN);

        lenient().when(metrics.getUserCreationCounter()).thenReturn(userCreationCounter);

    }

    @Test
    void authenticate_shouldReturnSuccess_whenUserExists_andPasswordMatches() {
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.ofNullable(user));
        when(encoder.matches(PASSWORD, HASH)).thenReturn(true);
        User authenticatedUser = userService.authenticate(USERNAME, PASSWORD);
        assertEquals(user, authenticatedUser);
        verify(userRepository).findByUsernameIgnoreCase(USERNAME);
        verify(encoder).matches(PASSWORD, HASH);
    }

    @Test
    void authenticate_shouldThrow_whenUserDoesNotExists() {
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> userService.authenticate(USERNAME, PASSWORD));
        verify(userRepository).findByUsernameIgnoreCase(USERNAME);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void authenticate_shouldThrow_whenPasswordDoesNotMatch() {
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.ofNullable(user));
        when(encoder.matches(PASSWORD, HASH)).thenReturn(false);
        assertThrows(BadCredentialsException.class, () -> userService.authenticate(USERNAME, PASSWORD));
        verify(userRepository).findByUsernameIgnoreCase(USERNAME);
        verify(encoder).matches(PASSWORD, HASH);
    }

    @Test
    void authenticate_shouldThrow_whenUsernameIsNull() {
        assertThrows(BadCredentialsException.class, () -> userService.authenticate(null, PASSWORD));
        verifyNoInteractions(userRepository, encoder);
    }

    @Test
    void authenticate_shouldThrow_whenUsernameIsBlank() {
        assertThrows(BadCredentialsException.class, () -> userService.authenticate("  ", PASSWORD));
        verifyNoInteractions(userRepository, encoder);
    }

    @Test
    void authenticate_shouldThrow_whenPasswordIsNull() {
        assertThrows(BadCredentialsException.class, () -> userService.authenticate(USERNAME, null));
        verifyNoInteractions(userRepository, encoder);
    }

    @Test
    void rolesByUsername_shouldReturnUserRoles() {
        User testUser = new User();
        testUser.setUsername(USERNAME);
        testUser.addRole(UserRole.USER);
        testUser.addRole(UserRole.ADMIN);

        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(testUser));

        SortedSet<String> roles = userService.rolesByUsername(USERNAME);

        assertEquals(2, roles.size());
        assertTrue(roles.contains("ADMIN"));
        assertTrue(roles.contains("USER"));
        assertEquals("ADMIN", roles.first());
        verify(userRepository).findByUsernameIgnoreCase(USERNAME);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void scopesByUsername_shouldReturnScopes() {
        user.removeRole(UserRole.ADMIN);
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user));
        List<String> scopesByUsername = userService.scopesByUsername(USERNAME);
        assertNotNull(scopesByUsername);
        assertFalse(scopesByUsername.isEmpty());
        assertTrue(scopesByUsername.stream().anyMatch(scopes -> scopes.contains(":read")));
        assertFalse(scopesByUsername.stream().anyMatch(scopes -> scopes.contains(":write")));
    }

    @Test
    void createUser_shouldSaveUser_withEncodedPassword() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encoder.encode(PASSWORD)).thenReturn(HASH);

        UserRequest request = new UserRequest(USERNAME, PASSWORD, Set.of(UserRole.USER, UserRole.ADMIN));
        User savedUser = userService.createUser(request);
        assertEquals(USERNAME, savedUser.getUsername());
        assertEquals(HASH, savedUser.getPasswordHash());
        assertTrue(savedUser.getRoles().contains(UserRole.USER));
        assertTrue(savedUser.getRoles().contains(UserRole.ADMIN));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User userToSave = captor.getValue();
        assertEquals(USERNAME, userToSave.getUsername());
        assertEquals(HASH, userToSave.getPasswordHash());
        assertTrue(userToSave.getRoles().contains(UserRole.USER));
        assertTrue(userToSave.getRoles().contains(UserRole.ADMIN));

        verify(userRepository).existsByUsername(USERNAME);
        verify(userRepository).save(userToSave);
        verify(encoder).encode(PASSWORD);
        verifyNoMoreInteractions(userRepository, encoder);

    }

    @Test
    void patchUser_shouldLockUser() {
        UserPatchRequest request = new UserPatchRequest(true, "Suspicious activity", null, null);
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.patchUser(USERNAME, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertTrue(savedUser.isLocked());
        assertEquals(0, savedUser.getFailedLoginAttempts()); // Reset when locking
    }

    @Test
    void patchUser_shouldResetAttempts() {
        UserPatchRequest request = new UserPatchRequest(null, null, true, null);
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.patchUser(USERNAME, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertEquals(0, savedUser.getFailedLoginAttempts());
        assertNull(savedUser.getLastFailedLogin());
    }
}
package com.demo.auth.service;

import com.demo.auth.dto.CreateUserRequest;
import com.demo.auth.entity.User;
import com.demo.auth.repository.UserRepository;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private User user;
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String HASH = "$hash";
    private static final String ROLES = "ROLE1,ROLE2";

    @Mock
    private PasswordEncoder encoder;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @BeforeEach
    void setUp() {
        user = new User(null, USERNAME, HASH, ROLES);
    }

    @Test
    void authenticate_shouldReturnSuccess_whenUserExists_andPasswordMatches() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.ofNullable(user));
        when(encoder.matches(PASSWORD, HASH)).thenReturn(true);
        User authenticatedUser = userService.authenticate(USERNAME, PASSWORD);
        assertEquals(user, authenticatedUser);
        verify(userRepository).findByUsername(USERNAME);
        verifyNoMoreInteractions(userRepository);
        verify(encoder).matches(PASSWORD, HASH);
        verifyNoMoreInteractions(encoder);
    }

    @Test
    void authenticate_shouldThrow_whenUserDoesNotExists() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> userService.authenticate(USERNAME, PASSWORD));
        verify(userRepository).findByUsername(USERNAME);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void authenticate_shouldThrow_whenPasswordDoesNotMatch() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.ofNullable(user));
        when(encoder.matches(PASSWORD, HASH)).thenReturn(false);
        assertThrows(BadCredentialsException.class, () -> userService.authenticate(USERNAME, PASSWORD));
        verify(userRepository).findByUsername(USERNAME);
        verifyNoMoreInteractions(userRepository);
        verify(encoder).matches(PASSWORD, HASH);
        verifyNoMoreInteractions(encoder);
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
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.ofNullable(user));
        List<String> roles = userService.rolesByUsername(USERNAME);
        assertArrayEquals(Arrays.stream(ROLES.split(",")).toArray(), roles.toArray());
        verify(userRepository).findByUsername(USERNAME);
        verifyNoMoreInteractions(userRepository);
    }

    @Disabled
    @Test
    void scopesByUsername_shouldReturnScopes() {
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

        CreateUserRequest request = new CreateUserRequest(USERNAME, PASSWORD, ROLES);
        User savedUser = userService.createUser(request);
        assertEquals(USERNAME, savedUser.getUsername());
        assertEquals(HASH, savedUser.getPasswordHash());
        assertEquals(ROLES, savedUser.getRoles());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User userToSave = captor.getValue();
        assertEquals(USERNAME, userToSave.getUsername());
        assertEquals(HASH, userToSave.getPasswordHash());
        assertEquals(ROLES, userToSave.getRoles());


        verify(userRepository).existsByUsername(USERNAME);
        verify(userRepository).save(userToSave);
        verify(encoder).encode(PASSWORD);
        verifyNoMoreInteractions(userRepository, encoder);

    }
}
package com.demo.auth.service;

import com.demo.auth.dto.UserAddressRequest;
import com.demo.auth.entity.User;
import com.demo.auth.entity.UserAddress;
import com.demo.auth.entity.UserAddressLabel;
import com.demo.auth.mapper.UserAddressMapper;
import com.demo.auth.mapper.UserAddressMapperImpl;
import com.demo.auth.repository.UserAddressRepository;
import com.demo.auth.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    private static final String USERNAME = "username";
    private static final String LASTNAME = "lastname";
    private static final UUID ADDRESS_ID = UUID.randomUUID();

    @Mock
    UserRepository userRepository;

    @Mock
    UserAddressRepository userAddressRepository;

    private UserAddressService userAddressService;
    private User user;
    private UserAddressRequest userAddressRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername(USERNAME);

        userAddressRequest = new UserAddressRequest(UserAddressLabel.HOME, USERNAME, LASTNAME, "+33600000000", "12 rue de la Paix", "Paris", "75001", "FR");

        UserAddressMapper userAddressMapper = new UserAddressMapperImpl();
        userAddressService = new UserAddressService(userAddressRepository, userAddressMapper, userRepository);
    }

    @Test
    void create_shouldMapAllFieldsFromRequest() {
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user));
        when(userAddressRepository.existsByUserUsernameIgnoreCase(USERNAME)).thenReturn(false);
        when(userAddressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userAddressService.createAddress(USERNAME, userAddressRequest);

        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressRepository).save(captor.capture());
        var saved = captor.getValue();
        assertEquals(UserAddressLabel.HOME, saved.getLabel());
        assertEquals(USERNAME, saved.getFirstName());
        assertEquals(LASTNAME, saved.getLastName());
        assertEquals("+33600000000", saved.getPhoneNumber());
        assertEquals("12 rue de la Paix", saved.getStreet());
        assertEquals("Paris", saved.getCity());
        assertEquals("75001", saved.getPostalCode());
        assertEquals("FR", saved.getCountry());
        assertEquals(user, saved.getUser());
        assertTrue(saved.isDefaultAddress());
    }

    @Test
    void getAddresses() {
    }
}

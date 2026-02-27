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
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
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
        private UserAddress address;
        private UserAddressRequest userAddressRequest;

        @BeforeEach
        void setUp() {
                user = new User();
                user.setUsername(USERNAME);

                UserAddressMapper userAddressMapper = new UserAddressMapperImpl();
                userAddressRequest = new UserAddressRequest(UserAddressLabel.HOME, USERNAME, LASTNAME, "+33600000000",
                                "12 rue de la Paix", "Paris", "75001", "FR");

                address = userAddressMapper.toUserAddress(userAddressRequest);
                address.setId(ADDRESS_ID);
                address.setUser(user);
                address.setDefaultAddress(true);

                userAddressService = new UserAddressService(userAddressRepository, userAddressMapper, userRepository);
        }

        @Test
        void getAddresses_shouldReturnMappedList() {
                when(userAddressRepository.findByUserUsernameIgnoreCase(USERNAME)).thenReturn(List.of(address));
                when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

                var result = userAddressService.getAddresses(USERNAME);

                assertEquals(1, result.size());
                var response = result.get(0);
                assertEquals(ADDRESS_ID, response.id());
                assertEquals(UserAddressLabel.HOME, response.label());
                assertEquals(USERNAME, response.firstName());
                assertEquals("FR", response.country());
                assertTrue(response.defaultAddress());
        }

        @Test
        void getAddresses_shouldReturnEmptyList_whenNoAddresses() {
                when(userAddressRepository.findByUserUsernameIgnoreCase(USERNAME)).thenReturn(List.of());
                when(userRepository.existsByUsername(USERNAME)).thenReturn(true);
                var result = userAddressService.getAddresses(USERNAME);

                assertTrue(result.isEmpty());
        }

        @Test
        void create_shouldMapAllFieldsFromRequest() {
                when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user));
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
        }

        @Test
        void create_shouldSetDefaultAddressTrue_whenFirstAddress() {
                when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user));
                when(userAddressRepository.existsByUserUsernameIgnoreCase(USERNAME)).thenReturn(false);
                when(userAddressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                userAddressService.createAddress(USERNAME, userAddressRequest);

                ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
                verify(userAddressRepository).save(captor.capture());
                assertTrue(captor.getValue().isDefaultAddress());
        }

        @Test
        void create_shouldSetDefaultFalse_whenNotFirstAddress() {
                when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user));
                when(userAddressRepository.existsByUserUsernameIgnoreCase(USERNAME)).thenReturn(true);
                when(userAddressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                userAddressService.createAddress(USERNAME, userAddressRequest);

                ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
                verify(userAddressRepository).save(captor.capture());
                assertFalse(captor.getValue().isDefaultAddress());
        }

        @Test
        void create_shouldThrow_whenUserNotFound() {
                when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());
                assertThrows(EntityNotFoundException.class,
                                () -> userAddressService.createAddress(USERNAME, userAddressRequest));
                verifyNoMoreInteractions(userRepository);
                verifyNoInteractions(userAddressRepository);
        }

        @Test
        void update_shouldApplyChanges_whenAddressFound() {
                when(userAddressRepository.findByIdAndUserUsernameIgnoreCase(ADDRESS_ID, USERNAME))
                                .thenReturn(Optional.of(address));
                var newRequest = new UserAddressRequest(
                                UserAddressLabel.WORK, "Bob", "Martin", null,
                                "1 avenue des Champs", "Lyon", "69001", "FR");
                userAddressService.update(ADDRESS_ID, USERNAME, newRequest);

                assertEquals(UserAddressLabel.WORK, address.getLabel());
                assertEquals("Bob", address.getFirstName());
                assertEquals("Lyon", address.getCity());
        }

        @Test
        void update_shouldThrow_whenAddressNotFound() {
                var newRequest = new UserAddressRequest(
                                UserAddressLabel.WORK, "Bob", "Martin", null,
                                "1 avenue des Champs", "Lyon", "69001", "FR");
                when(userAddressRepository.findByIdAndUserUsernameIgnoreCase(ADDRESS_ID, USERNAME))
                                .thenReturn(Optional.empty());

                assertThrows(EntityNotFoundException.class,
                                () -> userAddressService.update(ADDRESS_ID, USERNAME, newRequest));
                verify(userAddressRepository, never()).save(any());
        }

        @Test
        void delete_shouldDelete_andNotPromote_whenAddressWasNotDefault() {
                address.setDefaultAddress(false);
                when(userAddressRepository.findByIdAndUserUsernameIgnoreCase(ADDRESS_ID, USERNAME))
                                .thenReturn(Optional.of(address));

                userAddressService.delete(ADDRESS_ID, USERNAME);

                verify(userAddressRepository).delete(address);
                verify(userAddressRepository, never()).findByUserUsernameIgnoreCase(any());
        }

        @Test
        void delete_shouldPromoteNext_whenDeletedAddressWasDefault() {
                address.setDefaultAddress(true);
                var deleted = address;

                UserAddress next = new UserAddress();
                next.setId(UUID.randomUUID());
                next.setDefaultAddress(false);

                when(userAddressRepository.findByIdAndUserUsernameIgnoreCase(ADDRESS_ID, USERNAME))
                                .thenReturn(Optional.of(deleted));
                when(userAddressRepository.findByUserUsernameIgnoreCase(USERNAME))
                                .thenReturn(List.of(next));
                when(userAddressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                userAddressService.delete(ADDRESS_ID, USERNAME);

                verify(userAddressRepository).delete(deleted);
                assertTrue(next.isDefaultAddress());
        }

        @Test
        void delete_shouldThrow_whenAddressNotFound() {
                when(userAddressRepository.findByIdAndUserUsernameIgnoreCase(ADDRESS_ID, USERNAME))
                                .thenReturn(Optional.empty());

                assertThrows(EntityNotFoundException.class,
                                () -> userAddressService.delete(ADDRESS_ID, USERNAME));
                verify(userAddressRepository, never()).delete(any(UserAddress.class));
        }

        @Test
        void setDefault_shouldClearOldDefault_andSetNew() {
                UserAddress oldDefault = new UserAddress();
                oldDefault.setId(UUID.randomUUID());
                oldDefault.setDefaultAddress(true);

                address.setDefaultAddress(false);
                var newDefault = address;

                when(userAddressRepository.findDefaultAddressByUsername(USERNAME))
                                .thenReturn(Optional.of(oldDefault));
                when(userAddressRepository.findByIdAndUserUsernameIgnoreCase(ADDRESS_ID, USERNAME))
                                .thenReturn(Optional.of(newDefault));
                when(userAddressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                userAddressService.setDefault(ADDRESS_ID, USERNAME);

                assertFalse(oldDefault.isDefaultAddress());
                assertTrue(newDefault.isDefaultAddress());
        }

        @Test
        void setDefault_shouldThrow_whenAddressNotFound() {
                when(userAddressRepository.findByIdAndUserUsernameIgnoreCase(ADDRESS_ID, USERNAME))
                                .thenReturn(Optional.empty());

                assertThrows(EntityNotFoundException.class,
                                () -> userAddressService.setDefault(ADDRESS_ID, USERNAME));
        }

        @Test
        void create_shouldThrow_whenDuplicateAddressExists() {
                when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user));
                when(userAddressRepository.existsByFullAddressAndUsername(
                                anyString(), anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(true);

                assertThrows(IllegalArgumentException.class,
                                () -> userAddressService.createAddress(USERNAME, userAddressRequest));
        }

}

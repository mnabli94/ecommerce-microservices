package com.demo.auth.service;

import com.demo.auth.dto.UserAddressRequest;
import com.demo.auth.dto.UserAddressResponse;
import com.demo.auth.entity.User;
import com.demo.auth.entity.UserAddress;
import com.demo.auth.mapper.UserAddressMapper;
import com.demo.auth.repository.UserAddressRepository;
import com.demo.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserAddressMapper userAddressMapper;
    private final UserRepository userRepository;

    public UserAddressService(UserAddressRepository userAddressRepository, UserAddressMapper userAddressMapper,
            UserRepository userRepository) {
        this.userAddressRepository = userAddressRepository;
        this.userAddressMapper = userAddressMapper;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserAddressResponse createAddress(String username, UserAddressRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new EntityNotFoundException("No user found with username %s".formatted(username)));

        boolean isFirst = !userAddressRepository.existsByUserUsernameIgnoreCase(username);

        boolean exists = userAddressRepository.existsByFullAddressAndUsername(
                request.street(), request.city(), request.postalCode(), request.country(), username);

        if (exists) {
            log.warn("Duplicate address for user {}: {} in {}", username, request.street(), request.city());
            throw new IllegalArgumentException("Address already exists for this user");
        }

        UserAddress userAddress = userAddressMapper.toUserAddress(request);
        userAddress.setUser(user);
        userAddress.setDefaultAddress(isFirst);

        UserAddress saved = userAddressRepository.save(userAddress);
        log.info("Address created: id={}, user={}, default={}", saved.getId(), username, saved.isDefaultAddress());
        return userAddressMapper.toUserAddressResponse(saved);
    }

    public List<UserAddressResponse> getAddresses(String username) {
        if (!userRepository.existsByUsername(username)) {
            throw new EntityNotFoundException("No user found with username %s".formatted(username));
        }
        return userAddressRepository.findByUserUsernameIgnoreCase(username)
                .stream()
                .map(userAddressMapper::toUserAddressResponse)
                .toList();
    }

    @Transactional
    public UserAddressResponse update(UUID id, String username, @Valid UserAddressRequest request) {
        UserAddress userAddress = findOwnedOrThrow(id, username);
        userAddressMapper.update(userAddress, request);
        // userAddressRepository.save(userAddress);
        log.info("Address updated: id={}, user={}", id, username);
        return userAddressMapper.toUserAddressResponse(userAddress);
    }

    private UserAddress findOwnedOrThrow(UUID id, String username) {
        return userAddressRepository.findByIdAndUserUsernameIgnoreCase(id, username)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Address not found with id %s and username %s".formatted(id, username)));
    }

    @Transactional
    public void delete(UUID id, String username) {
        var address = findOwnedOrThrow(id, username);
        boolean wasDefault = address.isDefaultAddress();
        userAddressRepository.delete(address);
        log.info("Address deleted: id={}, user={}", id, username);

        if (wasDefault) {
            userAddressRepository.findByUserUsernameIgnoreCase(username)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefaultAddress(true);
                        userAddressRepository.save(next);
                        log.info("Promoted address id={} to default after deletion", next.getId());
                    });
        }
    }

    @Transactional
    public UserAddressResponse setDefault(UUID id, String username) {
        UserAddress userAddress = findOwnedOrThrow(id, username);

        userAddressRepository.findDefaultAddressByUsername(username)
                .ifPresent(current -> {
                    current.setDefaultAddress(false);
                    userAddressRepository.save(current);
                });
        userAddress.setDefaultAddress(true);
        var saved = userAddressRepository.save(userAddress);
        log.info("Default address set: id={}, user={}", id, username);
        return userAddressMapper.toUserAddressResponse(saved);
    }
}

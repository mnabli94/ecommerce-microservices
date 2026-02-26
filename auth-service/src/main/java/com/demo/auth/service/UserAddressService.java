package com.demo.auth.service;

import com.demo.auth.dto.UserAddressRequest;
import com.demo.auth.dto.UserAddressResponse;
import com.demo.auth.entity.User;
import com.demo.auth.entity.UserAddress;
import com.demo.auth.mapper.UserAddressMapper;
import com.demo.auth.repository.UserAddressRepository;
import com.demo.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public UserAddress createAddress(String username, UserAddressRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new EntityNotFoundException("No user found with username %s".formatted(username)));

        boolean isDefaultAddress = !userAddressRepository.existsByUserUsernameIgnoreCase(username);

        UserAddress userAddress = userAddressMapper.toUserAddress(request);
        userAddress.setUser(user);
        userAddress.setDefaultAddress(isDefaultAddress);

        return userAddressRepository.save(userAddress);
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
}

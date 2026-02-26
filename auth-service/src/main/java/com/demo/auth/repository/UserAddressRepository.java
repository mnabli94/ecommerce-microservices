package com.demo.auth.repository;

import com.demo.auth.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findByUserUsernameIgnoreCase(String username);

    boolean existsByUserUsernameIgnoreCase(String username);
}

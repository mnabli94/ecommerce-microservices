package com.demo.auth.repository;

import com.demo.auth.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findByUserUsernameIgnoreCase(String username);

    boolean existsByUserUsernameIgnoreCase(String username);

    Optional<UserAddress> findByIdAndUserUsernameIgnoreCase(UUID id, String username);

    @Query("SELECT ua FROM UserAddress ua" +
            " WHERE UPPER(ua.user.username) = UPPER(:username) " +
            "AND ua.defaultAddress = true")
    Optional<UserAddress> findDefaultAddressByUsername(String username);

    @Query("SELECT COUNT(ua) > 0 FROM UserAddress ua " +
            "WHERE UPPER(ua.street) = UPPER(:street) " +
            "AND UPPER(ua.city) = UPPER(:city) " +
            "AND UPPER(ua.postalCode) = UPPER(:postalCode) " +
            "AND UPPER(ua.country) = UPPER(:country) " +
            "AND UPPER(ua.user.username) = UPPER(:username)")
    boolean existsByFullAddressAndUsername(String street, String city, String postalCode, String country,
            String username);
}

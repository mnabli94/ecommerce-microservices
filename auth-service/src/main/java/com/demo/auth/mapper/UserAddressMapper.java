package com.demo.auth.mapper;

import com.demo.auth.dto.UserAddressRequest;
import com.demo.auth.dto.UserAddressResponse;
import com.demo.auth.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserAddressMapper {

    UserAddressResponse toUserAddressResponse(UserAddress userAddress);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "defaultAddress", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserAddress toUserAddress(UserAddressRequest request);
}

package com.demo.order.service;

import com.demo.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSecurityServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderSecurityService orderSecurityService;

    private final UUID ORDER_ID = UUID.randomUUID();
    private final String USERNAME = "username";

    @Test
    void isOwner_shouldReturnTrue_whenUserOwnsOrder() {
         when(orderRepository.existsByIdAndUserId(ORDER_ID, USERNAME)).thenReturn(true);

        boolean result = orderSecurityService.isOwner(ORDER_ID, USERNAME);

        assertTrue(result);
        verify(orderRepository).existsByIdAndUserId(ORDER_ID, USERNAME);
    }

    @Test
    void isOwner_shouldReturnFalse_whenUserDoesNotOwnOrder() {
        when(orderRepository.existsByIdAndUserId(ORDER_ID, USERNAME)).thenReturn(false);

        boolean result = orderSecurityService.isOwner(ORDER_ID, USERNAME);

        assertFalse(result);
        verify(orderRepository).existsByIdAndUserId(ORDER_ID, USERNAME);
    }

    @Test
    void isOwner_shouldReturnFalse_whenOrderDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.existsByIdAndUserId(unknownId, USERNAME)).thenReturn(false);

        boolean result = orderSecurityService.isOwner(unknownId, USERNAME);

        assertFalse(result);
    }

    @Test
    void isOwner_shouldReturnFalse_whenDifferentUserTriesToAccessOrder() {
        String anotherUser = "other_user";
        when(orderRepository.existsByIdAndUserId(ORDER_ID, anotherUser)).thenReturn(false);

        boolean result = orderSecurityService.isOwner(ORDER_ID, anotherUser);

        // Then
        assertFalse(result);
        verify(orderRepository).existsByIdAndUserId(ORDER_ID, anotherUser);
    }
}

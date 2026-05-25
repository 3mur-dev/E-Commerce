package com.omar.ecommerce.services;

import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.entities.UserStatus;
import com.omar.ecommerce.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_marksDisabledUsersAsDisabled() {
        User user = new User();
        user.setUsername("omar");
        user.setPassword("encoded");
        user.setRole(Role.USER);
        user.setStatus(UserStatus.DISABLED);

        when(userRepository.findByUsername("omar")).thenReturn(Optional.of(user));

        var details = customUserDetailsService.loadUserByUsername("omar");

        assertFalse(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }
}

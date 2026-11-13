package com.pm.authservice.service;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.model.User;
import com.pm.authservice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTests {
    private final UserService userService = mock(UserService.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AuthService service = new AuthService(userService, encoder, jwtUtil);

    @Test
    void correctPasswordReturnsTokenForStoredUser() {
        User user = storedUser();
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtUtil.generateTokens(user.getEmail(), user.getRole())).thenReturn("test-token");

        assertEquals(Optional.of("test-token"), service.authenticate(request("correct-password")));
        verify(jwtUtil).generateTokens(user.getEmail(), user.getRole());
    }

    @Test
    void wrongPasswordDoesNotGenerateToken() {
        User user = storedUser();
        when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertTrue(service.authenticate(request("wrong-password")).isEmpty());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void unknownEmailDoesNotGenerateToken() {
        when(userService.findByEmail("auth-test@example.com")).thenReturn(Optional.empty());

        assertTrue(service.authenticate(request("correct-password")).isEmpty());
        verifyNoInteractions(jwtUtil);
    }

    private User storedUser() {
        User user = new User();
        user.setEmail("auth-test@example.com");
        user.setPassword(encoder.encode("correct-password"));
        user.setRole("USER");
        return user;
    }

    private LoginRequestDTO request(String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("auth-test@example.com");
        request.setPassword(password);
        return request;
    }
}

package com.belikesnab.peach;

import com.belikesnab.peach.dto.AuthResponse;
import com.belikesnab.peach.dto.LoginRequest;
import com.belikesnab.peach.dto.MessageResponse;
import com.belikesnab.peach.dto.RegisterRequest;
import com.belikesnab.peach.entity.User;
import com.belikesnab.peach.repository.UserRepository;
import com.belikesnab.peach.security.JwtUtils;
import com.belikesnab.peach.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User tester;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        tester = new User("tester", "tester@peach.com", "encodedPassword");

        tester.setId(1L);
        tester.setRoles(Set.of("USER"));
        tester.setAccountNonLocked(true);
        tester.setFailedLoginAttempts(0);

        registerRequest = new RegisterRequest("new_user", "new@peach.com", "password123");
        loginRequest = new LoginRequest("tester", "password123");
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(tester);

        MessageResponse response = authService.register(registerRequest);

        assertEquals("User registered successfully", response.message());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameExists() {
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("Username is already taken", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("Email is already in use", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() {
        when(userRepository.findByUsername(loginRequest.username())).thenReturn(Optional.of(tester));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwt(authentication)).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenReturn(tester);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("tester", response.username());
        assertEquals(0, tester.getFailedLoginAttempts());

        verify(userRepository, times(1)).save(tester);
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void shouldThrowExceptionForInvalidCredentials() {
        when(userRepository.findByUsername(loginRequest.username())).thenReturn(Optional.of(tester));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

        verify(userRepository, times(1)).save(tester);

        assertEquals(1, tester.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should lock account after 5 failed attempts")
    void shouldLockAccountAfterFailedAttempts() {
        tester.setFailedLoginAttempts(4);

        when(userRepository.findByUsername(loginRequest.username())).thenReturn(Optional.of(tester));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

        assertEquals(5, tester.getFailedLoginAttempts());
        assertFalse(tester.isAccountNonLocked());

        verify(userRepository, times(1)).save(tester);
    }

    @Test
    @DisplayName("Should throw exception when account is locked")
    void shouldThrowExceptionWhenAccountLocked() {
        tester.setAccountNonLocked(false);

        when(userRepository.findByUsername(loginRequest.username())).thenReturn(Optional.of(tester));

        LockedException exception = assertThrows(
                LockedException.class,
                () -> authService.login(loginRequest)
        );

        assertTrue(exception.getMessage().contains("locked"));

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Should reset failed attempts on successful login")
    void shouldResetFailedAttemptsOnSuccess() {
        tester.setFailedLoginAttempts(3);

        when(userRepository.findByUsername(loginRequest.username())).thenReturn(Optional.of(tester));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwt(authentication)).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenReturn(tester);

        authService.login(loginRequest);

        assertEquals(0, tester.getFailedLoginAttempts());
        assertNotNull(tester.getLastLogin());
    }
}

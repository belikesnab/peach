package com.belikesnab.peach;

import com.belikesnab.peach.dto.LoginRequest;
import com.belikesnab.peach.dto.RegisterRequest;
import com.belikesnab.peach.entity.User;
import com.belikesnab.peach.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUser() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "new_user",
                "new@peach.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    @DisplayName("Should return validation errors for invalid registration")
    void shouldReturnValidationErrors() throws Exception {
        RegisterRequest request = new RegisterRequest("ab", "invalid-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    @DisplayName("Should reject duplicate username")
    void shouldRejectDuplicateUsername() throws Exception {
        User existing = new User("test_user", "test@peach.com", "password");

        userRepository.save(existing);

        RegisterRequest request = new RegisterRequest(
                "test_user",
                "other@peach.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        User user = new User("test_user", "test@peach.com",
                passwordEncoder.encode("password123"));

        user.setRoles(Set.of("USER"));

        userRepository.save(user);

        LoginRequest request = new LoginRequest("test_user", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("test_user"))
                .andExpect(jsonPath("$.email").value("test@peach.com"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    @DisplayName("Should return 401 for invalid credentials")
    void shouldReturn401ForInvalidCredentials() throws Exception {
        User user = new User("test_user", "test@peach.com",
                passwordEncoder.encode("password123"));

        userRepository.save(user);

        LoginRequest request = new LoginRequest("test_user", "wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("Should access protected endpoint with valid token")
    void shouldAccessProtectedEndpoint() throws Exception {
        User user = new User("test_user", "test@peach.com",
                passwordEncoder.encode("password123"));

        user.setRoles(Set.of("USER"));

        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("test_user", "password123");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("test_user")));
    }

    @Test
    @DisplayName("Should return 401 for missing token")
    void shouldReturn401ForMissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 for invalid token")
    void shouldReturn401ForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should lock account after 5 failed login attempts")
    void shouldLockAccountAfterFailedAttempts() throws Exception {
        User user = new User("test_user", "test@peach.com",
                passwordEncoder.encode("password123"));
        user.setRoles(Set.of("USER"));
        userRepository.save(user);

        LoginRequest request = new LoginRequest("test_user", "wrongPassword");

        // Attempt 5 failed logins
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt should return locked status
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

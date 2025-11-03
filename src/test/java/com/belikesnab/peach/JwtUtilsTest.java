package com.belikesnab.peach;

import com.belikesnab.peach.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilsTest {
    private JwtUtils jwtUtils;
    private UserDetails userDetails;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();

        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
                "f7f61f7547b5ef811b77d6cf30d27dcfb0ab52bc0e8f74a876643c07fcb94d71");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000);

        userDetails = User.withUsername("tester")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidJwtToken() {
        String token = jwtUtils.generateJwt(authentication);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length); // jwt has 3 parts
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {
        String token = jwtUtils.generateJwt(authentication);
        String username = jwtUtils.getUsernameFromJwt(token);

        assertEquals("tester", username);
    }

    @Test
    @DisplayName("Should validate correct token")
    void shouldValidateCorrectToken() {
        String token = jwtUtils.generateJwt(authentication);

        assertTrue(jwtUtils.validateJwt(token));
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        String invalidToken = "e4ee5a4806bc200984cd8d2717627100302514515b0a1d850a03f21d1e517edd";

        assertFalse(jwtUtils.validateJwt(invalidToken));
    }

    @Test
    @DisplayName("Should generate token from username")
    void shouldGenerateTokenFromUsername() {
        String token = jwtUtils.generateTokenFromUsername("tester");
        String username = jwtUtils.getUsernameFromJwt(token);

        assertEquals("tester", username);
    }
}

package com.belikesnab.peach;

import com.belikesnab.peach.entity.User;
import com.belikesnab.peach.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    private User tester;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        tester = new User("tester", "tester@peach.com", "password");

        tester.setRoles(Set.of("USER"));

        tester = userRepository.save(tester);
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        Optional<User> found = userRepository.findByUsername("tester");

        assertTrue(found.isPresent());
        assertEquals("tester", found.get().getUsername());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        Optional<User> found = userRepository.findByEmail("tester@peach.com");

        assertTrue(found.isPresent());
        assertEquals("tester@peach.com", found.get().getEmail());
    }

    @Test
    @DisplayName("Should return empty when username not found")
    void shouldReturnEmptyWhenUsernameNotFound() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should check if username exists")
    void shouldCheckIfUsernameExists() {
        assertTrue(userRepository.existsByUsername("tester"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        assertTrue(userRepository.existsByEmail("tester@peach.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@peach.com"));
    }

    @Test
    @DisplayName("Should enforce unique username constraint")
    void shouldEnforceUniqueUsernameConstraint() {
        User duplicate = new User("tester", "other@peach.com", "password");

        assertThrows(Exception.class, () -> {
            userRepository.save(duplicate);
            userRepository.flush();
        });
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void shouldEnforceUniqueEmailConstraint() {
        User duplicate = new User("other", "tester@peach.com", "password");

        assertThrows(Exception.class, () -> {
            userRepository.save(duplicate);
            userRepository.flush();
        });
    }

    @Test
    @DisplayName("Should persist user roles")
    void shouldPersistUserRoles() {
        User user = new User("role_user", "role@peach.com", "password");

        user.setRoles(Set.of("USER", "ADMIN"));

        user = userRepository.save(user);

        Optional<User> found = userRepository.findById(user.getId());

        assertTrue(found.isPresent());
        assertEquals(2, found.get().getRoles().size());
        assertTrue(found.get().getRoles().contains("USER"));
        assertTrue(found.get().getRoles().contains("ADMIN"));
    }
}

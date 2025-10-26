package com.belikesnab.peach.dto;

import java.util.Set;

public record AuthResponse(
        String token,
        String type,
        Long id,
        String username,
        String email,
        Set<String> roles
) {
    public AuthResponse(String token, Long id, String username, String email, Set<String> roles) {
        this(token, "Bearer", id, username, email, roles);
    }
}

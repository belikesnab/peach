package com.belikesnab.peach.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record UserInfoResponse(
        Long id,
        String username,
        String email,
        Set<String> roles,
        boolean enabled,
        boolean accountNonLocked,
        LocalDateTime lastLogin,
        LocalDateTime createdAt
) {
}

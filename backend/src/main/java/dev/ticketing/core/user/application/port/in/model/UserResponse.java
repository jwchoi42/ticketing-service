package dev.ticketing.core.user.application.port.in.model;

import dev.ticketing.core.user.domain.User;
import dev.ticketing.core.user.domain.UserRole;

public record UserResponse(Long id, String email, UserRole role) {

    public static UserResponse from(final User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}

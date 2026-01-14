package dev.ticketing.core.user.application.port.in.model;

import dev.ticketing.core.user.domain.User;

public record UserResponse(Long id, String email) {

    public static UserResponse from(final User user) {
        return new UserResponse(user.getId(), user.getEmail());
    }
}

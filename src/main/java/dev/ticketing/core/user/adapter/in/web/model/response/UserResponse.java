package dev.ticketing.core.user.adapter.in.web.model.response;

import dev.ticketing.core.user.domain.User;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
        @Schema(description = "사용자 ID", example = "1") Long id,
        @Schema(description = "이메일", example = "user@example.com") String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail());
    }
}

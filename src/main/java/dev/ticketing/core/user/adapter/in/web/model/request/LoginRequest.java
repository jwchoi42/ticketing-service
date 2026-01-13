package dev.ticketing.core.user.adapter.in.web.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "비밀번호", example = "password123") String password) {
}

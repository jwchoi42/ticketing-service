package dev.ticketing.common.web.model.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

public record SuccessResponse<T>(
        @JsonInclude(JsonInclude.Include.NON_NULL) String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) T data,
        LocalDateTime timestamp) {

    public static <T> SuccessResponse<T> of(final T data) {
        return new SuccessResponse<>(null, data, LocalDateTime.now());
    }

    public static <T> SuccessResponse<T> of(final String message, final T data) {
        return new SuccessResponse<>(message, data, LocalDateTime.now());
    }

    public static SuccessResponse<Void> of(final String message) {
        return new SuccessResponse<>(message, null, LocalDateTime.now());
    }

    public static SuccessResponse<Void> empty() {
        return new SuccessResponse<>(null, null, LocalDateTime.now());
    }
}

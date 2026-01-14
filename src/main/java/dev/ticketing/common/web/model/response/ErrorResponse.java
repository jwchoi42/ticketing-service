package dev.ticketing.common.web.model.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ErrorResponse(
        @JsonInclude(JsonInclude.Include.NON_NULL) String message,
        LocalDateTime timestamp) {

    public static ErrorResponse of(final String message) {
        return new ErrorResponse(message, LocalDateTime.now());
    }
}

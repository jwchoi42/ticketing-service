package dev.ticketing.common.web.model.response;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ErrorResponse(
        @JsonInclude(JsonInclude.Include.NON_NULL) String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> errors,
        LocalDateTime timestamp) {

    public static ErrorResponse of(final String message) {
        return new ErrorResponse(message, null, LocalDateTime.now());
    }

    public static ErrorResponse of(final String message, final Map<String, String> errors) {
        return new ErrorResponse(message, errors, LocalDateTime.now());
    }
}

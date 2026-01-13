package dev.ticketing.common.web.model.response;

public record ErrorResponse(
        Integer status,
        String message) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message);
    }
}

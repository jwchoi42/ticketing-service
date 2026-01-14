package dev.ticketing.common.web.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record SuccessResponse<T>(
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer status,
        @JsonInclude(JsonInclude.Include.NON_NULL) T data) {

    public static <T> SuccessResponse<T> of(int status, T data) {
        return new SuccessResponse<>(status, data);
    }

    public static <T> SuccessResponse<T> of(T data) {
        return new SuccessResponse<>(200, data);
    }

    public static SuccessResponse<Void> empty() {
        return new SuccessResponse<>(null, null);
    }
}

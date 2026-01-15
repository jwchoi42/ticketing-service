package dev.ticketing.common.web.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

import dev.ticketing.common.exception.DomainException;
import dev.ticketing.common.web.model.response.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(final DomainException e) {
        log.warn("Unhandled domain exception caught by GlobalControllerAdvice: {}", e.getClass().getSimpleName());
        return ResponseEntity
                .status(e.getStatus())
                .body(ErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpectedException(final Exception e) {
        log.error("Unexpected error occurred", e);
        return ErrorResponse.of("Internal Server Error");
    }
}

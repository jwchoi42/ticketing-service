package dev.ticketing.core.match.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.ticketing.common.web.model.response.ErrorResponse;
import dev.ticketing.core.match.application.service.exception.MatchNotFoundException;

@RestControllerAdvice(basePackages = "dev.ticketing.core.match")
public class MatchControllerAdvice {

    @ExceptionHandler(MatchNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleMatchNotFoundException(final MatchNotFoundException e) {
        return ErrorResponse.of(e.getMessage());
    }
}

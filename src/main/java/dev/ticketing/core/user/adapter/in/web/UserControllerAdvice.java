package dev.ticketing.core.user.adapter.in.web;

import dev.ticketing.common.web.model.response.ErrorResponse;
import dev.ticketing.core.user.application.service.exception.DuplicateEmailException;
import dev.ticketing.core.user.application.service.exception.LoginFailureException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "dev.ticketing.core.user")
public class UserControllerAdvice {

    @ExceptionHandler(LoginFailureException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleLoginFailureException(LoginFailureException e) {
        return ErrorResponse.of(401, e.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateEmailException(DuplicateEmailException e) {
        return ErrorResponse.of(409, e.getMessage());
    }
}

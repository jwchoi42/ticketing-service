package dev.ticketing.core.site.adapter.in.web.allocation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.ticketing.common.web.model.response.ErrorResponse;
import dev.ticketing.core.site.application.service.exception.*;

@RestControllerAdvice
public class AllocationControllerAdvice {

    @ExceptionHandler(SeatAlreadyHeldException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleSeatAlreadyHeld(SeatAlreadyHeldException e) {
        return ErrorResponse.of(HttpStatus.CONFLICT.value(), e.getMessage());
    }

    @ExceptionHandler(SeatAlreadyOccupiedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleSeatAlreadyOccupied(SeatAlreadyOccupiedException e) {
        return ErrorResponse.of(HttpStatus.CONFLICT.value(), e.getMessage());
    }

    @ExceptionHandler(UnauthorizedSeatReleaseException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleUnauthorizedRelease(UnauthorizedSeatReleaseException e) {
        return ErrorResponse.of(HttpStatus.FORBIDDEN.value(), e.getMessage());
    }

    @ExceptionHandler(SeatNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleSeatNotFound(SeatNotFoundException e) {
        return ErrorResponse.of(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }

    @ExceptionHandler(AllocationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAllocationNotFound(AllocationNotFoundException e) {
        return ErrorResponse.of(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }

    @ExceptionHandler(NoSeatsToConfirmException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNoSeatsToConfirm(NoSeatsToConfirmException e) {
        return ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

}

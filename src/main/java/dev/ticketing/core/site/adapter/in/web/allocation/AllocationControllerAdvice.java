package dev.ticketing.core.site.adapter.in.web.allocation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.ticketing.common.web.model.response.ErrorResponse;
import dev.ticketing.core.site.application.service.exception.AllocationNotFoundException;
import dev.ticketing.core.site.application.service.exception.NoSeatsToConfirmException;
import dev.ticketing.core.site.application.service.exception.SeatAlreadyHeldException;
import dev.ticketing.core.site.application.service.exception.SeatAlreadyOccupiedException;
import dev.ticketing.core.site.application.service.exception.SeatNotFoundException;
import dev.ticketing.core.site.application.service.exception.UnauthorizedSeatReleaseException;

@RestControllerAdvice(basePackages = "dev.ticketing.core.site")
public class AllocationControllerAdvice {

    @ExceptionHandler(SeatAlreadyHeldException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleSeatAlreadyHeld(final SeatAlreadyHeldException e) {
        return ErrorResponse.of(e.getMessage());
    }

    @ExceptionHandler(SeatAlreadyOccupiedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleSeatAlreadyOccupied(final SeatAlreadyOccupiedException e) {
        return ErrorResponse.of(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedSeatReleaseException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleUnauthorizedRelease(final UnauthorizedSeatReleaseException e) {
        return ErrorResponse.of(e.getMessage());
    }

    @ExceptionHandler(SeatNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleSeatNotFound(final SeatNotFoundException e) {
        return ErrorResponse.of(e.getMessage());
    }

    @ExceptionHandler(AllocationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAllocationNotFound(final AllocationNotFoundException e) {
        return ErrorResponse.of(e.getMessage());
    }

    @ExceptionHandler(NoSeatsToConfirmException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNoSeatsToConfirm(final NoSeatsToConfirmException e) {
        return ErrorResponse.of(e.getMessage());
    }
}

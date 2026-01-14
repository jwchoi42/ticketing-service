package dev.ticketing.core.site.application.service.exception;

public class SeatAlreadyHeldException extends RuntimeException {
    public SeatAlreadyHeldException(String message) {
        super(message);
    }

    public SeatAlreadyHeldException(Long matchId, Long seatId) {
        super(String.format("Seat %d is already held for match %d", seatId, matchId));
    }
}

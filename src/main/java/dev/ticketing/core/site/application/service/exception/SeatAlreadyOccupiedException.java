package dev.ticketing.core.site.application.service.exception;

/**
 * 좌석이 이미 확정(OCCUPIED)된 경우 발생하는 예외
 */
public class SeatAlreadyOccupiedException extends RuntimeException {
    public SeatAlreadyOccupiedException(String message) {
        super(message);
    }

    public SeatAlreadyOccupiedException(Long matchId, Long seatId) {
        super(String.format("Seat %d is already occupied for match %d", seatId, matchId));
    }
}

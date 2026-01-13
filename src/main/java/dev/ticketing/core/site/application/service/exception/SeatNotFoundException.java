package dev.ticketing.core.site.application.service.exception;

/**
 * 좌석을 찾을 수 없는 경우 발생하는 예외
 */
public class SeatNotFoundException extends RuntimeException {
    public SeatNotFoundException(String message) {
        super(message);
    }

    public SeatNotFoundException(Long seatId) {
        super(String.format("Seat not found: %d", seatId));
    }
}

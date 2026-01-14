package dev.ticketing.core.site.application.service.exception;

/**
 * 권한 없는 사용자가 좌석 해제를 시도하는 경우 발생하는 예외
 */
public class UnauthorizedSeatReleaseException extends RuntimeException {
    public UnauthorizedSeatReleaseException(String message) {
        super(message);
    }

    public UnauthorizedSeatReleaseException(Long matchId, Long seatId, Long userId) {
        super(String.format("User %d is not authorized to release seat %d for match %d", userId, seatId, matchId));
    }
}

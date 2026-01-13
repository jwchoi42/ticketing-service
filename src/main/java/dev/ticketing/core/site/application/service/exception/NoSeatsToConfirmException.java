package dev.ticketing.core.site.application.service.exception;

import lombok.Getter;

import java.util.List;

/**
 * 좌석 확정 시 점유한 좌석이 없는 경우 발생하는 예외
 */
@Getter
public class NoSeatsToConfirmException extends RuntimeException {
    private final Long userId;
    private final Long matchId;
    private final List<Long> requestedSeatIds;

    public NoSeatsToConfirmException(Long userId, Long matchId, List<Long> requestedSeatIds) {
        super(String.format("No seats to confirm for user %d in match %d. Requested seats: %s",
                userId, matchId, requestedSeatIds));
        this.userId = userId;
        this.matchId = matchId;
        this.requestedSeatIds = requestedSeatIds;
    }
}

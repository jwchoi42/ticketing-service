package dev.ticketing.core.site.application.service.exception;

/**
 * 좌석 배정 정보를 찾을 수 없는 경우 발생하는 예외
 */
public class AllocationNotFoundException extends RuntimeException {
    public AllocationNotFoundException(String message) {
        super(message);
    }

    public AllocationNotFoundException(Long matchId, Long seatId) {
        super(String.format("Allocation not found for seat %d in match %d", seatId, matchId));
    }
}

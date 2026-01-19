package dev.ticketing.core.reservation.domain;

/**
 * 예약 상태
 */
public enum ReservationStatus {
    /**
     * 예약 대기 중 (좌석 점유 완료, 결제 대기)
     */
    PENDING,

    /**
     * 예약 확정 (결제 완료)
     */
    CONFIRMED,

    /**
     * 예약 취소
     */
    CANCELLED
}

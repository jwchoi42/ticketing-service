package dev.ticketing.core.payment.domain;

/**
 * 결제 상태
 */
public enum PaymentStatus {
    /**
     * 결제 대기 중
     */
    PENDING,

    /**
     * 결제 완료
     */
    PAID,

    /**
     * 결제 실패
     */
    FAILED,

    /**
     * 결제 취소/환불
     */
    REFUNDED
}

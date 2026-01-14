package dev.ticketing.core.site.domain.allocation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Allocation {

    private Long id;
    private Long userId;
    private Long matchId;
    private Long seatId;
    private Long reservationId;
    private AllocationStatus status;
    private LocalDateTime holdExpiresAt;
    private LocalDateTime updatedAt;

    // 기존 팩토리 메서드 호환성 유지 (필요에 따라 업데이트)
    public static Allocation of(Long seatId, AllocationStatus status) {
        return Allocation.builder()
                .seatId(seatId)
                .status(status)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Allocation available(Long seatId) {
        return Allocation.builder()
                .seatId(seatId)
                .status(AllocationStatus.AVAILABLE)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Allocation withId(Long id, Long userId, Long matchId, Long seatId, Long reservationId, AllocationStatus status, LocalDateTime holdExpiresAt, LocalDateTime updatedAt) {
        return Allocation.builder()
                .id(id)
                .userId(userId)
                .matchId(matchId)
                .seatId(seatId)
                .reservationId(reservationId)
                .status(status)
                .holdExpiresAt(holdExpiresAt)
                .updatedAt(updatedAt)
                .build();
    }

    public boolean isHeldBy(Long userId) {
        return this.status == AllocationStatus.HOLD && this.userId != null && this.userId.equals(userId);
    }
}

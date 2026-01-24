package dev.ticketing.core.site.domain.allocation;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public static Allocation of(final Long seatId, final AllocationStatus status) {
        return Allocation.builder()
                .seatId(seatId)
                .status(status)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Allocation available(final Long seatId) {
        return Allocation.builder()
                .seatId(seatId)
                .status(AllocationStatus.AVAILABLE)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Allocation availableForMatch(final Long matchId, final Long seatId) {
        return Allocation.builder()
                .matchId(matchId)
                .seatId(seatId)
                .status(AllocationStatus.AVAILABLE)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Allocation withId(
            final Long id,
            final Long userId,
            final Long matchId,
            final Long seatId,
            final Long reservationId,
            final AllocationStatus status,
            final LocalDateTime holdExpiresAt,
            final LocalDateTime updatedAt) {
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

    public boolean isHeldBy(final Long userId) {
        return this.status == AllocationStatus.HOLD && this.userId != null && this.userId.equals(userId);
    }

    public Allocation hold(final Long userId, final Long matchId, final LocalDateTime expiresAt) {
        return Allocation.builder()
                .id(this.id)
                .userId(userId)
                .matchId(matchId)
                .seatId(this.seatId)
                .reservationId(null)
                .status(AllocationStatus.HOLD)
                .holdExpiresAt(expiresAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Allocation release() {
        return Allocation.builder()
                .id(this.id)
                .userId(null)
                .matchId(this.matchId)
                .seatId(this.seatId)
                .reservationId(null)
                .status(AllocationStatus.AVAILABLE)
                .holdExpiresAt(null)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Allocation occupy() {
        return Allocation.builder()
                .id(this.id)
                .userId(this.userId)
                .matchId(this.matchId)
                .seatId(this.seatId)
                .reservationId(this.reservationId)
                .status(AllocationStatus.OCCUPIED)
                .holdExpiresAt(null)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Allocation assignReservation(final Long reservationId) {
        return Allocation.builder()
                .id(this.id)
                .userId(this.userId)
                .matchId(this.matchId)
                .seatId(this.seatId)
                .reservationId(reservationId)
                .status(this.status)
                .holdExpiresAt(this.holdExpiresAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

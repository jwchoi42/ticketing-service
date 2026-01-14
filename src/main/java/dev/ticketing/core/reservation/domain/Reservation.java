package dev.ticketing.core.reservation.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Reservation {
    private Long id;
    private Long userId;
    private Long matchId;
    private ReservationStatus status;
    private List<Long> seatIds;

    public static Reservation create(Long userId, Long matchId, ReservationStatus status) {
        return Reservation.builder()
                .userId(userId)
                .matchId(matchId)
                .status(status)
                .build();
    }

    public static Reservation withId(Long id, Long userId, Long matchId, ReservationStatus status) {
        return Reservation.builder()
                .id(id)
                .userId(userId)
                .matchId(matchId)
                .status(status)
                .build();
    }

    public static Reservation withSeatIds(Long id, Long userId, Long matchId, ReservationStatus status,
            List<Long> seatIds) {
        return Reservation.builder()
                .id(id)
                .userId(userId)
                .matchId(matchId)
                .status(status)
                .seatIds(seatIds)
                .build();
    }
}

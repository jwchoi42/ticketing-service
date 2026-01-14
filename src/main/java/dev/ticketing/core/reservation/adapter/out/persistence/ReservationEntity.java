package dev.ticketing.core.reservation.adapter.out.persistence;

import dev.ticketing.core.reservation.domain.Reservation;
import dev.ticketing.core.reservation.domain.ReservationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "reservations")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long matchId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    public static ReservationEntity from(final Reservation reservation) {
        return new ReservationEntity(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getMatchId(),
                reservation.getStatus());
    }

    public Reservation toDomain() {
        return Reservation.withId(id, userId, matchId, status);
    }
}

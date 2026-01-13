package dev.ticketing.core.reservation.adapter.in.web;

import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.reservation.adapter.in.web.model.CreateReservationRequest;
import dev.ticketing.core.reservation.application.port.in.CreateReservationUseCase;
import dev.ticketing.core.reservation.domain.Reservation;
import dev.ticketing.core.site.application.port.out.persistence.allocation.LoadAllocationPort;
import dev.ticketing.core.site.domain.allocation.Allocation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reservation", description = "예약 API")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;
    private final dev.ticketing.core.reservation.application.port.out.persistence.LoadReservationPort loadReservationPort;
    private final LoadAllocationPort loadAllocationPort;

    @Operation(summary = "예약 생성 (좌석 선택 완료 후)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<Reservation> createReservation(@RequestBody CreateReservationRequest request) {
        Reservation reservation = createReservationUseCase.createReservation(request.toCommand());
        return SuccessResponse.of(reservation);
    }

    @Operation(summary = "예약 조회")
    @GetMapping("/{id}")
    public SuccessResponse<Reservation> getReservation(@PathVariable Long id) {
        Reservation reservation = loadReservationPort.loadById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        // Load seatIds from allocations
        List<Allocation> allocations = loadAllocationPort.loadAllocationsByReservationId(id);
        List<Long> seatIds = allocations.stream()
                .map(Allocation::getSeatId)
                .toList();

        Reservation reservationWithSeats = Reservation.withSeatIds(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getMatchId(),
                reservation.getStatus(),
                seatIds);

        return SuccessResponse.of(reservationWithSeats);
    }
}

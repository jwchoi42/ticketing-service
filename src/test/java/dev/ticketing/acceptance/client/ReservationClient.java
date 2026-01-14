package dev.ticketing.acceptance.client;

import dev.ticketing.acceptance.client.model.BaseClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.util.Map;
import java.util.List;

public class ReservationClient extends BaseClient {
    public ReservationClient(WebTestClient webTestClient) {
        super(webTestClient);
    }

    public TestResponse createReservation(Long userId, Long matchId, List<Long> seatIds) {
        return toTestResponse(webTestClient.post()
                .uri("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", userId, "matchId", matchId, "seatIds", seatIds))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse getReservation(Long reservationId) {
        return toTestResponse(webTestClient.get()
                .uri("/api/reservations/{reservationId}", reservationId)
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse getReservationsByUserId(Long userId) {
        return toTestResponse(webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reservations")
                        .queryParam("userId", userId)
                        .build())
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse cancelReservation(Long reservationId, Long userId) {
        return toTestResponse(webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/api/reservations/{reservationId}")
                        .queryParam("userId", userId)
                        .build(reservationId))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }
}

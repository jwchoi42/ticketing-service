package dev.ticketing.acceptance.client;

import dev.ticketing.acceptance.client.model.BaseClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.util.Map;

public class AllocationClient extends BaseClient {
    public AllocationClient(WebTestClient webTestClient) {
        super(webTestClient);
    }

    public TestResponse holdSeat(Long matchId, Long seatId, Long userId) {
        return toTestResponse(webTestClient.post()
                .uri("/api/matches/{matchId}/allocation/seats/{seatId}/hold", 
                        matchId, seatId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", userId))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse releaseSeat(Long matchId, Long seatId, Long userId) {
        return toTestResponse(webTestClient.post()
                .uri("/api/matches/{matchId}/allocation/seats/{seatId}/release", 
                        matchId, seatId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", userId))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse confirmSeats(Long matchId, Long userId) {
        return toTestResponse(webTestClient.post()
                .uri("/api/matches/{matchId}/allocation/seats/confirm", matchId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", userId))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }
}

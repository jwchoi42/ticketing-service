package dev.ticketing.acceptance.client;

import dev.ticketing.acceptance.client.model.BaseClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

public class AdminClient extends BaseClient {

    public AdminClient(WebTestClient webTestClient) {
        super(webTestClient);
    }

    public TestResponse createMatch(Long userId, String stadium, String homeTeam, String awayTeam, String dateTime) {
        return toTestResponse(webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/admin/matches")
                        .queryParam("userId", userId)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "stadium", stadium,
                        "homeTeam", homeTeam,
                        "awayTeam", awayTeam,
                        "dateTime", dateTime
                ))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse updateMatch(Long userId, Long matchId, String stadium, String homeTeam, String awayTeam, String dateTime) {
        return toTestResponse(webTestClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/admin/matches/{matchId}")
                        .queryParam("userId", userId)
                        .build(matchId))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "stadium", stadium,
                        "homeTeam", homeTeam,
                        "awayTeam", awayTeam,
                        "dateTime", dateTime
                ))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse deleteMatch(Long userId, Long matchId) {
        return toTestResponse(webTestClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/admin/matches/{matchId}")
                        .queryParam("userId", userId)
                        .build(matchId))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse openMatch(Long userId, Long matchId) {
        return toTestResponse(webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/admin/matches/{matchId}/open")
                        .queryParam("userId", userId)
                        .build(matchId))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }
}

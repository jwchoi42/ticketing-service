package dev.ticketing.acceptance.client;

import dev.ticketing.acceptance.client.model.BaseClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import org.springframework.test.web.reactive.server.WebTestClient;

public class MatchClient extends BaseClient {
    public MatchClient(WebTestClient webTestClient) {
        super(webTestClient);
    }

    public TestResponse getMatches() {
        return toTestResponse(webTestClient.get()
                .uri("/api/matches")
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse getMatch(Long matchId) {
        return toTestResponse(webTestClient.get()
                .uri("/api/matches/{matchId}", matchId)
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }
}

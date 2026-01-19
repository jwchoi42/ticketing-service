package dev.ticketing.acceptance.client;

import dev.ticketing.acceptance.client.model.BaseClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

public class AllocationStatusClient extends BaseClient {
    public AllocationStatusClient(WebTestClient webTestClient) {
        super(webTestClient);
    }

    public Flux<ServerSentEvent<String>> subscribeSeatStatusStream(Long matchId, Long blockId) {
        return webTestClient.get()
                .uri("/api/matches/{matchId}/blocks/{blockId}/seats/events", matchId, blockId)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .getResponseBody();
    }

    public TestResponse getSeatStatuses(Long matchId, Long blockId) {
        return toTestResponse(webTestClient.get()
                .uri("/api/matches/{matchId}/blocks/{blockId}/seats", matchId, blockId)
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }
}

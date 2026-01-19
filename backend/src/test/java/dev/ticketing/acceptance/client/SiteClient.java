package dev.ticketing.acceptance.client;

import dev.ticketing.acceptance.client.model.BaseClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import org.springframework.test.web.reactive.server.WebTestClient;

public class SiteClient extends BaseClient {
    public SiteClient(WebTestClient webTestClient) {
        super(webTestClient);
    }

    public TestResponse getAreas() {
        return toTestResponse(webTestClient.get()
                .uri("/api/site/areas")
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse getSections(Long areaId) {
        return toTestResponse(webTestClient.get()
                .uri("/api/site/areas/{areaId}/sections", areaId)
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse getBlocks(Long sectionId) {
        return toTestResponse(webTestClient.get()
                .uri("/api/site/sections/{sectionId}/blocks", sectionId)
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse getSeats(Long blockId) {
        return toTestResponse(webTestClient.get()
                .uri("/api/site/blocks/{blockId}/seats", blockId)
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }
}

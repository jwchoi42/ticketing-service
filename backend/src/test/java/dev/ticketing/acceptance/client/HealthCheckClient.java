package dev.ticketing.acceptance.client;

import dev.ticketing.acceptance.client.model.BaseClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import org.springframework.test.web.reactive.server.WebTestClient;

public class HealthCheckClient extends BaseClient {
    public HealthCheckClient(WebTestClient webTestClient) {
        super(webTestClient);
    }

    public TestResponse performHealthCheck() {
        return toTestResponse(webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }
}

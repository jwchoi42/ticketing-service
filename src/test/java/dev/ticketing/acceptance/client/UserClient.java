package dev.ticketing.acceptance.client;

import dev.ticketing.acceptance.client.model.BaseClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.util.Map;

public class UserClient extends BaseClient {
    public UserClient(WebTestClient webTestClient) {
        super(webTestClient);
    }

    public TestResponse signUp(String email, String password) {
        return toTestResponse(webTestClient.post()
                .uri("/api/users/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse logIn(String email, String password) {
        return toTestResponse(webTestClient.post()
                .uri("/api/users/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }
}

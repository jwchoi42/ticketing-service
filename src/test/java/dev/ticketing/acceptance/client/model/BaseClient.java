package dev.ticketing.acceptance.client.model;

import lombok.RequiredArgsConstructor;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

@RequiredArgsConstructor
public abstract class BaseClient {
    protected final WebTestClient webTestClient;

    protected TestResponse toTestResponse(EntityExchangeResult<byte[]> result) {
        return new TestResponse(
                result.getStatus().value(),
                result.getResponseBody()
        );
    }
}

package dev.ticketing.acceptance.client;

import dev.ticketing.acceptance.client.model.BaseClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.util.Map;

public class PaymentClient extends BaseClient {
    public PaymentClient(WebTestClient webTestClient) {
        super(webTestClient);
    }

    public TestResponse requestPayment(Long reservationId, Integer amount, String method) {
        return toTestResponse(webTestClient.post()
                .uri("/api/payments/request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("reservationId", reservationId, "amount", amount, "method", method))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse confirmPayment(Long paymentId, String paymentKey, String orderId, Integer amount) {
        return toTestResponse(webTestClient.post()
                .uri("/api/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("paymentId", paymentId, "paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }

    public TestResponse getPayment(Long paymentId) {
        return toTestResponse(webTestClient.get()
                .uri("/api/payments/{paymentId}", paymentId)
                .exchange()
                .expectBody(byte[].class)
                .returnResult());
    }
}

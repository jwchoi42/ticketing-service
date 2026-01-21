package dev.ticketing.acceptance.config;

import dev.ticketing.acceptance.client.*;
import dev.ticketing.acceptance.context.TestContext;
import dev.ticketing.configuration.TestContainerConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@CucumberContextConfiguration
@Import({TestContainerConfiguration.class, AcceptanceTestConfig.AcceptanceTestInnerConfig.class})
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
public class AcceptanceTestConfig {

    @TestConfiguration
    public static class AcceptanceTestInnerConfig {

        @Bean
        @ScenarioScope
        public TestContext testContext() {
            return new TestContext();
        }

        @Bean
        public WebTestClientBuilderCustomizer webTestClientBuilderCustomizer() {
            return builder -> builder.exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                    .build());
        }

        @Lazy
        @Bean
        public MatchClient matchClient(WebTestClient webTestClient) {
            return new MatchClient(webTestClient);
        }

        @Lazy
        @Bean
        public SiteClient siteClient(WebTestClient webTestClient) {
            return new SiteClient(webTestClient);
        }

        @Lazy
        @Bean
        public AllocationClient allocationClient(WebTestClient webTestClient) {
            return new AllocationClient(webTestClient);
        }

        @Lazy
        @Bean
        public ReservationClient reservationClient(WebTestClient webTestClient) {
            return new ReservationClient(webTestClient);
        }

        @Lazy
        @Bean
        public PaymentClient paymentClient(WebTestClient webTestClient) {
            return new PaymentClient(webTestClient);
        }

        @Lazy
        @Bean
        public UserClient userClient(WebTestClient webTestClient) {
            return new UserClient(webTestClient);
        }

        @Lazy
        @Bean
        public AllocationStatusClient statusStreamClient(WebTestClient webTestClient) {
            return new AllocationStatusClient(webTestClient);
        }

        @Lazy
        @Bean
        public HealthCheckClient healthClient(WebTestClient webTestClient) {
            return new HealthCheckClient(webTestClient);
        }

        @Lazy
        @Bean
        public AdminClient adminClient(WebTestClient webTestClient) {
            return new AdminClient(webTestClient);
        }
    }
}

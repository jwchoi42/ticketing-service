package dev.ticketing.acceptance.cucumber.configuration;

import dev.ticketing.acceptance.cucumber.CucumberTestApiClient;
import dev.ticketing.acceptance.cucumber.CucumberTestContext;
import io.cucumber.spring.CucumberContextConfiguration;
import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@CucumberContextConfiguration
@Import(CucumberTestContainersConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class CucumberSpringConfiguration {

    @TestConfiguration
    static class CucumberTestConfig {

        @Bean
        @ScenarioScope
        public CucumberTestContext cucumberTestContext() {
            return new CucumberTestContext();
        }

        @Lazy
        @Bean
        public CucumberTestApiClient cucumberTestApiClient(CucumberTestContext context, Environment env) {
            int port = env.getProperty("local.server.port", Integer.class);
            String baseUrl = "http://localhost:" + port;
            return new CucumberTestApiClient(baseUrl, context);
        }
    }
}

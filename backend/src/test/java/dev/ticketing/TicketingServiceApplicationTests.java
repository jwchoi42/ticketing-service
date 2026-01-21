package dev.ticketing;

import dev.ticketing.configuration.TestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestContainerConfiguration.class)
class TicketingServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}

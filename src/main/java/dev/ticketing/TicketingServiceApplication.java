package dev.ticketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TicketingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketingServiceApplication.class, args);
    }

}

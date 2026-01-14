package dev.ticketing.core.user.application.port.in.model;

public record SignUpCommand(String email, String password) {
}

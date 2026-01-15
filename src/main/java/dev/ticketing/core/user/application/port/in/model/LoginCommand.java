package dev.ticketing.core.user.application.port.in.model;

public record LoginCommand(String email, String password) {
}

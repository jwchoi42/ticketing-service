package dev.ticketing.core.user.application.port.out;

import dev.ticketing.core.user.domain.User;

import java.util.Optional;

public interface LoadUserPort {
    Optional<User> loadById(Long userId);

    Optional<User> loadByEmail(String email);
}

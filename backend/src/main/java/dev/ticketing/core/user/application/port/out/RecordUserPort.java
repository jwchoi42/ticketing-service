package dev.ticketing.core.user.application.port.out;

import dev.ticketing.core.user.domain.User;

public interface RecordUserPort {
    User record(User user);

    boolean existsByEmail(String email);
}

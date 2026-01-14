package dev.ticketing.core.user.application.port.in;

import dev.ticketing.core.user.application.port.in.model.SignUpCommand;
import dev.ticketing.core.user.domain.User;

public interface SignUpUseCase {
    User signUp(SignUpCommand command);
}

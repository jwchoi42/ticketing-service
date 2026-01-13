package dev.ticketing.core.user.application.port.in;

import dev.ticketing.core.user.application.port.in.model.LoginCommand;
import dev.ticketing.core.user.domain.User;

public interface LoginUseCase {
    User login(LoginCommand command);
}

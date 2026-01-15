package dev.ticketing.core.user.application.port.in;

import dev.ticketing.core.user.application.port.in.model.LoginCommand;
import dev.ticketing.core.user.application.port.in.model.UserResponse;

public interface LoginUseCase {

    UserResponse login(LoginCommand command);
}

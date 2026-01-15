package dev.ticketing.core.user.application.port.in;

import dev.ticketing.core.user.application.port.in.model.SignUpCommand;
import dev.ticketing.core.user.application.port.in.model.UserResponse;

public interface SignUpUseCase {

    UserResponse signUp(SignUpCommand command);
}

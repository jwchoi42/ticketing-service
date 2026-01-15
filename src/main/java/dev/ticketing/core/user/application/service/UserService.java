package dev.ticketing.core.user.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import dev.ticketing.core.user.application.port.in.LoginUseCase;
import dev.ticketing.core.user.application.port.in.SignUpUseCase;
import dev.ticketing.core.user.application.port.in.model.LoginCommand;
import dev.ticketing.core.user.application.port.in.model.SignUpCommand;
import dev.ticketing.core.user.application.port.in.model.UserResponse;
import dev.ticketing.core.user.application.port.out.LoadUserPort;
import dev.ticketing.core.user.application.port.out.RecordUserPort;
import dev.ticketing.core.user.application.service.exception.DuplicateEmailException;
import dev.ticketing.core.user.application.service.exception.LoginFailureException;
import dev.ticketing.core.user.domain.User;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements SignUpUseCase, LoginUseCase {

    private final LoadUserPort loadUserPort;
    private final RecordUserPort recordUserPort;

    @Override
    @Transactional
    public UserResponse signUp(final SignUpCommand command) {
        if (recordUserPort.existsByEmail(command.email())) {
            throw new DuplicateEmailException(command.email());
        }

        User user = User.create(command.email(), command.password());
        User saved = recordUserPort.record(user);
        return UserResponse.from(saved);
    }

    @Override
    public UserResponse login(final LoginCommand command) {
        User user = loadUserPort.loadByEmail(command.email())
                .orElseThrow(() -> new LoginFailureException("User not found: " + command.email()));

        if (!user.matchPassword(command.password())) {
            throw new LoginFailureException("Invalid password");
        }

        return UserResponse.from(user);
    }
}

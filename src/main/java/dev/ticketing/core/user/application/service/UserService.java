package dev.ticketing.core.user.application.service;

import dev.ticketing.core.user.application.port.in.LoginUseCase;
import dev.ticketing.core.user.application.port.in.SignUpUseCase;
import dev.ticketing.core.user.application.port.in.model.LoginCommand;
import dev.ticketing.core.user.application.port.in.model.SignUpCommand;
import dev.ticketing.core.user.application.port.out.LoadUserPort;
import dev.ticketing.core.user.application.port.out.RecordUserPort;
import dev.ticketing.core.user.application.service.exception.DuplicateEmailException;
import dev.ticketing.core.user.application.service.exception.LoginFailureException;
import dev.ticketing.core.user.domain.User;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements SignUpUseCase, LoginUseCase {

    private final LoadUserPort loadUserPort;
    private final RecordUserPort recordUserPort;

    @Override
    @Transactional
    public User signUp(SignUpCommand command) {
        if (recordUserPort.existsByEmail(command.email())) {
            throw new DuplicateEmailException("Email already exists: " + command.email());
        }

        User user = User.create(command.email(), command.password());
        return recordUserPort.record(user);
    }

    @Override
    public User login(LoginCommand command) {

        User user = loadUserPort.loadByEmail(command.email())
                .orElseThrow(() -> new LoginFailureException("User not found: " + command.email()));

        if (!user.getPassword().equals(command.password())) {
            throw new LoginFailureException("Invalid password");
        }

        return user;
    }
}

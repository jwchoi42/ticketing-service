package dev.ticketing.core.user.adapter.out.persistence;

import dev.ticketing.core.user.application.port.out.LoadUserPort;
import dev.ticketing.core.user.application.port.out.RecordUserPort;
import dev.ticketing.core.user.domain.User;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaUserPersistenceAdapter implements LoadUserPort, RecordUserPort {

    private final UserRepository userRepository;

    @Override
    public Optional<User> loadById(final Long userId) {
        return userRepository.findById(userId).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> loadByEmail(final String email) {
        return userRepository.findByEmail(email).map(UserEntity::toDomain);
    }

    @Override
    public User record(final User user) {
        UserEntity entity = UserEntity.from(user);
        return userRepository.save(entity).toDomain();
    }

    @Override
    public boolean existsByEmail(final String email) {
        return userRepository.existsByEmail(email);
    }
}

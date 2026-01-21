package dev.ticketing.core.user.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    private Long id;
    private String email;
    private String password;
    private UserRole role;

    public static User create(final String email, final String password) {
        return User.builder()
                .email(email)
                .password(password)
                .role(UserRole.USER)
                .build();
    }

    public static User create(final String email, final String password, final UserRole role) {
        return User.builder()
                .email(email)
                .password(password)
                .role(role)
                .build();
    }

    public static User withId(final Long id, final String email, final String password, final UserRole role) {
        return User.builder()
                .id(id)
                .email(email)
                .password(password)
                .role(role)
                .build();
    }

    public boolean matchPassword(final String password) {
        return this.password.equals(password);
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
}

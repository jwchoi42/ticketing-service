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

    public static User create(String email, String password) {
        return User.builder()
                .email(email)
                .password(password)
                .build();
    }

    public static User withId(Long id, String email, String password) {
        return User.builder()
                .id(id)
                .email(email)
                .password(password)
                .build();
    }
}

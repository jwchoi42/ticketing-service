@user
Feature: 사용자 가입 및 로그인

  # 가입

  Scenario: 이메일과 비밀번호로 가입에 성공한다.
    When 이메일 "user@email.com"과 비밀번호 "password"로 가입하면,
    Then 가입에 성공한다.

  Scenario: 이미 등록된 이메일로는 가입에 실패한다.
    Given 이미 등록된 이메일 "signed-up@email.com"이 존재할 때,
    When 이메일 "signed-up@email.com"으로 가입하면,
    Then 가입에 실패한다.

  # 로그인

  Scenario: 이메일과 비밀번호로 로그인에 성공한다.
    Given 이메일 "user@email.com"과 비밀번호 "password"로 가입된 사용자가 존재할 때,
    When 이메일 "user@email.com"과 비밀번호 "password"로 로그인하면,
    Then 로그인에 성공한다.

  Scenario: 가입되지 않은 이메일로 로그인하면 인증에 실패한다.
    When 가입되지 않은 이메일 "not-signed-up-user@email.com"로 로그인하면,
    Then 인증에 실패한다.

  Scenario: 잘못된 비밀번호로 로그인하면 인증에 실패한다.
    Given 이메일 "user@email.com"과 비밀번호 "password"로 가입된 사용자가 존재할 때,
    When 잘못된 비밀번호 "wrong-password"로 로그인하면,
    Then 인증에 실패한다.

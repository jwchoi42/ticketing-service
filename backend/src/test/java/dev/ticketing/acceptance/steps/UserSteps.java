package dev.ticketing.acceptance.steps;

import dev.ticketing.acceptance.client.model.TestResponse;
import dev.ticketing.acceptance.client.UserClient;
import dev.ticketing.acceptance.context.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.cucumber.spring.ScenarioScope;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ScenarioScope
@RequiredArgsConstructor
public class UserSteps {

    private final UserClient userClient;
    private final TestContext context;

    // 가입

    @When("이메일 {string}과 비밀번호 {string}로 가입하면,")
    public void signUpWithEmailAndPassword(String email, String password) {
        TestResponse response = userClient.signUp(email, password);
        context.setResponse(response);
    }

    @Then("가입에 성공한다.")
    public void verifySignUpSuccess() {
        assertThat(context.getStatusCode()).isEqualTo(201);
    }

    @Given("이미 등록된 이메일 {string}이 존재할 때,")
    public void signedUpEmailExists(String email) {
        userClient.signUp(email, "password");
    }

    @When("이메일 {string}으로 가입하면,")
    public void signUpWithEmail(String email) {
        TestResponse response = userClient.signUp(email, "password");
        context.setResponse(response);
    }

    @Then("가입에 실패한다.")
    public void verifySignUpFailure() {
        assertThat(context.getStatusCode()).isEqualTo(409);
    }

    // 로그인

    @Given("이메일 {string}과 비밀번호 {string}로 가입된 사용자가 존재할 때,")
    public void signedUpUserExists(String email, String password) {
        userClient.signUp(email, password);
    }

    @When("이메일 {string}과 비밀번호 {string}로 로그인하면,")
    public void logIn(String email, String password) {
        TestResponse response = userClient.logIn(email, password);
        context.setResponse(response);
    }

    @Then("로그인에 성공한다.")
    public void verifyLogInSuccess() {
        assertThat(context.getStatusCode()).isEqualTo(200);
        Long userId = Long.valueOf(context.getStringFromJsonPath("data.id"));
        context.saveUserId(context.getStringFromJsonPath("data.email"), userId);
    }

    @When("가입되지 않은 이메일 {string}로 로그인하면,")
    public void logInWithNotSignedUpEmail(String email) {
        TestResponse response = userClient.logIn(email, "any-password");
        context.setResponse(response);
    }

    @When("잘못된 비밀번호 {string}로 로그인하면,")
    public void logInWithWrongPassword(String password) {
        TestResponse response = userClient.logIn("user@email.com", password);
        context.setResponse(response);
    }

    @Then("인증에 실패한다.")
    public void verifyAuthenticationFailure() {
        assertThat(context.getStatusCode()).isEqualTo(401);
    }

    //

    @Given("사용자가 이메일 {string}로 로그인하고,")
    public void userLoggedIn(String email) {
        userClient.signUp(email, "password");
        TestResponse response = userClient.logIn(email, "password");
        context.setResponse(response);
        assertThat(context.getStatusCode()).isEqualTo(200);
        Long userId = Long.valueOf(context.getStringFromJsonPath("data.id"));
        context.saveUserId(email, userId);
        context.setCurrentUserEmail(email);
    }

    @Given("다른 사용자가 이메일 {string}로 로그인하고,")
    public void anotherUserLoggedIn(String email) {
        userClient.signUp(email, "password");
        TestResponse response = userClient.logIn(email, "password");
        context.setResponse(response);
        assertThat(context.getStatusCode()).isEqualTo(200);
        Long userId = Long.valueOf(context.getStringFromJsonPath("data.id"));
        context.saveUserId(email, userId);
        context.setCurrentUserEmail(email);
    }
}

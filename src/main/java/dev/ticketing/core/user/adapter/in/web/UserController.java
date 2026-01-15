package dev.ticketing.core.user.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import dev.ticketing.common.web.model.response.SuccessResponse;
import dev.ticketing.core.user.adapter.in.web.model.request.LoginRequest;
import dev.ticketing.core.user.adapter.in.web.model.request.SignUpRequest;
import dev.ticketing.core.user.application.port.in.LoginUseCase;
import dev.ticketing.core.user.application.port.in.SignUpUseCase;
import dev.ticketing.core.user.application.port.in.model.LoginCommand;
import dev.ticketing.core.user.application.port.in.model.SignUpCommand;
import dev.ticketing.core.user.application.port.in.model.UserResponse;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SignUpUseCase signUpUseCase;
    private final LoginUseCase loginUseCase;

    @Operation(summary = "회원 가입", description = "이메일과 비밀번호로 새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")
    })
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<UserResponse> signUp(@RequestBody final SignUpRequest request) {
        SignUpCommand command = new SignUpCommand(request.email(), request.password());
        UserResponse response = signUpUseCase.signUp(command);
        return SuccessResponse.of(response);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/log-in")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<UserResponse> login(@RequestBody final LoginRequest request) {
        LoginCommand command = new LoginCommand(request.email(), request.password());
        UserResponse response = loginUseCase.login(command);
        return SuccessResponse.of(response);
    }
}

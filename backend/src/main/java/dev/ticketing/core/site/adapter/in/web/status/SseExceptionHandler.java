package dev.ticketing.core.site.adapter.in.web.status;

import dev.ticketing.common.web.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * SseExceptionHandler - SSE 및 AllocationStatusController 전용 예외 핸들러
 * 클라이언트의 Accept 헤더를 확인하여 SSE 요청인 경우 에러 이벤트를 전송하고,
 * 일반 요청인 경우 JSON 응답을 반환합니다.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = AllocationStatusController.class)
public class SseExceptionHandler {

    /**
     * SSE 연결 타임아웃 처리
     * 서버 설정 타임아웃 발생 시 호출됩니다. (정상적인 생명주기 종료 단계)
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        log.debug("SSE 연결 타임아웃 발생 (정상 종료)");
    }

    /**
     * 클라이언트 연결 끊김 및 입출력 예외 처리
     */
    @ExceptionHandler(IOException.class)
    public void handleIOException(IOException e, HttpServletRequest request) {
        if (isSseRequest(request)) {
            log.info("SSE 클라이언트 연결 끊김: {}", e.getMessage());
            return;
        }
        log.error("입출력 오류 발생", e);
    }

    /**
     * 미디어 타입 부적합 예외 처리 (Accept 헤더 충돌 방지)
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeException(HttpMediaTypeNotAcceptableException e) {
        log.warn("지원하지 않는 미디어 타입 요청: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of("지원하지 않는 응답 형식입니다."));
    }

    /**
     * 공통 예외 처리
     * SSE 요청이면 "error" 이벤트를 포함한 Emitter를 반환하고, 아니면 일반 ResponseEntity를 반환합니다.
     */
    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception e, HttpServletRequest request) {
        log.error("데이터 처리 중 예외 발생: {}", e.getMessage(), e);

        if (isSseRequest(request)) {
            SseEmitter emitter = new SseEmitter();
            try {
                // 클라이언트에서 리스닝할 수 있도록 "error" 이벤트 전송
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(ErrorResponse.of("실시간 데이터 조회 중 오류가 발생했습니다: " + e.getMessage())));
                emitter.complete();
            } catch (IOException ignored) {
                // 이미 연결이 끊긴 경우 무시
            }
            return emitter;
        }

        // 일반 API(SnapShot 등) 요청에 대한 처리
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of("Internal Server Error"));
    }

    private boolean isSseRequest(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}

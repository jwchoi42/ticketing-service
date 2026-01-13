---
trigger: model_decision
---

API Specification Rule
===

- **Base URL**: `/api`
- **Content-Type**: `application/json`

Response
---
- All API responses follow a consistent structure.
- Use Java records to ensure immutability and readability.

- Success Response:

```json
{
    "status": 200,
    "data": { ... } 
}
```
- Error Response

```json
{
    "status": 400,
    "message": "error message"
}
```

Response Templates
---

### SuccessResponse<T>
- **목적**: 성공 응답의 일관된 구조를 제공하는 템플릿
- **필드**:
  - `status` (Integer, nullable): HTTP 상태 코드
  - `data` (T, nullable): 응답 데이터
- **Swagger 문서화**: Controller에서 `@ApiResponse`로 명시적으로 정의

### ErrorResponse
- **목적**: 에러 응답의 일관된 구조를 제공하는 템플릿
- **필드**:
  - `status` (Integer): HTTP 상태 코드
  - `message` (String): 에러 메시지
- **Swagger 문서화**: Controller에서 `@ApiResponse`로 명시적으로 정의

SuccessResponse 사용 가이드
---

### 1. 데이터가 있는 경우
```java
return SuccessResponse.of(data);  // status: 200
return SuccessResponse.of(201, data);  // 커스텀 status
```

### 2. 데이터가 없는 경우 (No Content)
```java
// @ResponseStatus로 상태 코드 지정
@ResponseStatus(HttpStatus.NO_CONTENT)  // 204
public SuccessResponse<Void> someMethod() {
    return SuccessResponse.empty();  // status 필드 없음 (null)
}

@ResponseStatus(HttpStatus.CREATED)  // 201
public SuccessResponse<Void> anotherMethod() {
    return SuccessResponse.empty();  // status 필드 없음 (null)
}
```

**중요**: 
- `SuccessResponse.empty()`는 `status` 필드를 `null`로 반환하여 JSON에서 제외합니다.
- 실제 HTTP 상태 코드는 `@ResponseStatus` 어노테이션으로 제어합니다.
- 응답 본문: `{"data": null}` 또는 `{}`

**사용 예시**:
- `POST /seats/{seatId}/hold`: 좌석 점유 성공 → `SuccessResponse.of(null)` + `@ResponseStatus(HttpStatus.CREATED)`
- `POST /seats/{seatId}/release`: 좌석 반환 성공 → `SuccessResponse.empty()` + `@ResponseStatus(HttpStatus.NO_CONTENT)`

Exception Handling
---
- **400 Bad Request**: 요청 파라미터가 유효하지 않을 때
- **401 Unauthorized**: 인증 실패 (`LoginFailureException`)
- **403 Forbidden**: 권한 부족
- **404 Not Found**: 리소스가 없음
- **500 Internal Server Error**: 서버 오류

Swagger Documentation
---

### 기본 원칙
- `SuccessResponse`와 `ErrorResponse`는 **제네릭 템플릿**이므로 `@Schema` 어노테이션을 직접 사용하지 않습니다.
- 각 API 엔드포인트에서 `@ApiResponse`를 사용하여 **명시적으로 응답 스키마를 정의**합니다.
- API 명세서(`.agent/rules/api-spec-*.md`)의 내용을 바탕으로 문서화합니다.

### 1. @Tag (Controller Level)
- **name**: API 명세서의 최상위 제목에서 도메인 명칭을 추출하여 작성한다. (예: `# 사용자(User) API` -> `name = "User"`)
- **description**: 해당 도메인에 대한 간단한 설명을 작성한다. (예: `description = "사용자 API"`)
- **주의**: "~ 관련 API"가 아닌 "~ API" 형식으로 작성한다.

### 2. @Operation (Method Level)
- **summary**: API 명세서의 각 엔드포인트 제목을 작성한다. (예: `## 가입(Sign-Up)` -> `summary = "가입(Sign-Up)"`)
- **description**: API 명세서에 기술된 상세 설명이나 비즈니스 로직을 요약하여 작성한다.

### 3. @ApiResponse (Method Level)
- API 명세서의 `Response` 항목에 기재된 모든 상태 코드를 문서화한다.
- **responseCode**: HTTP 상태 코드 (200, 201, 400, 401 등)
- **description**: 해당 상태 코드가 발생하는 상황을 API 명세서의 내용을 참고하여 서술형으로 작성한다.
- **content**: 실제 응답 예시를 `@ExampleObject`로 제공한다.

#### 예시: 성공 응답 (데이터 포함)
```java
@Operation(summary = "좌석 선택 확정", description = "점유 중인 좌석들을 확정하여 예약을 진행합니다.")
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "좌석 확정 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = SuccessResponse.class),
            examples = @ExampleObject(value = """
                {
                  "status": 200,
                  "data": {
                    "confirmedSeats": [
                      {"seatId": 1, "status": "CONFIRMED"},
                      {"seatId": 2, "status": "CONFIRMED"}
                    ]
                  }
                }
                """)
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (점유된 좌석이 없음 등)",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"점유된 좌석이 없습니다.\"}")
        )
    )
})
```

#### 예시: 성공 응답 (No Content)
```java
@Operation(summary = "좌석 반환 (Release)", description = "점유 중인 좌석을 반환합니다.")
@ApiResponses({
    @ApiResponse(
        responseCode = "204",
        description = "좌석 반환 성공 (응답 본문 없음)"
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (점유하지 않은 좌석 등)",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"점유하지 않은 좌석입니다.\"}")
        )
    )
})
```

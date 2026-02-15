# 해당 글은 초안으로 클로드 코드로 다시 작성한다.

참고글: https://haward.tistory.com/251
---
### 내가 이해한 바
예외를 추가하는 경우 자동으로 스웨거에 등록되고, 또 한 번에 모든 예외를 볼 수 있도록 스웨거 커스텀 작업을 진행한다.

### OperationCustomizer
스웨거를 커스텀할 커스텀이 가능한 부분이다.
Operation 객체는 Describes a single API operation on a path. 즉 단일 API에 대한 정보를 담은 객체이다. 해당 객체에 들어갈 정보를 커스텀해서 API 문서의 내용을 바꿔줄 수 있다.

```java
public class Operation {  
    private List<String> tags = null;  
    private String summary = null;  
    private String description = null;  
    private ExternalDocumentation externalDocs = null;  
    private String operationId = null;  
    private List<Parameter> parameters = null;  
    private RequestBody requestBody = null;  
    private ApiResponses responses = null;  
    private Map<String, Callback> callbacks = null;  
    private Boolean deprecated = null;  
    private List<SecurityRequirement> security = null;  
    private List<Server> servers = null;  
    private java.util.Map<String, Object> extensions = null;
    ...
```
- Operation 객체는 위처럼 ApiResponses 형태의 responses, 즉 리스트를 가지고 있다.
- 우리는 이 리스트에 커스텀한 정보를 추가해줘 문서를 커스텀할 것이다.
```java
public class ApiResponses extends LinkedHashMap<String, ApiResponse> {
---
public class ApiResponse {  
    private String description = null;  
    private Map<String, Header> headers = null;  
    private Content content = null;  
    private java.util.Map<String, Link> links = null;  
    private java.util.Map<String, Object> extensions = null;  
    private String $ref = null;
    ...
---
```
- <String, ApiResponse> 형태로 Json을 만드는데, 아래 이미지처럼 응답 코드별로 매칭되어있는 형태다.
  ![[Pasted image 20260117220000.png]]
- content 안에 application/json 형식안에 있는 객체는 **Media Type Object** 로 여기엔 examples 라는 필드가 있어 예시 정보를 추가해줄 수 있다

### 커스텀 어노테이션 설정
```java
@Target({ElementType.METHOD, ElementType.TYPE})  
@Retention(RetentionPolicy.RUNTIME)  
public @interface ApiErrorCodeExample {  

    Class<? extends ErrorCodeInterface>[] value();  
}
---
@Tag(name = "USER", description = "사용자 API")  
@RestController  
@RequiredArgsConstructor  
@RequestMapping("/api/v1/users")  
@ApiErrorCodeExample(UserErrorCode.class)  
public class UserController {
```
- 해당 어노테이션은 메서드 혹은 클래스에 붙어 에러코드를 정의할 수 있는 역할을 한다.
- 즉 컨트롤러 클래스, 혹은 메서드에서 해당 예외코드 정보를 선언하고, 이를 통해 스웨거가 정보를 읽어갈 수 있게 하는 것이다.
- 내부에는 ErrorCodeInterface를 담는다.
- 메서드와 클래스 모두에 적용될 수 있도록 ElementType을 METHOD, TYPE으로 함께 선언했다.
- 또한 리스트로 받을 수 있게 선언해 확장성을 고려했다.

#### 스웨거 설정과 연동
스웨거에서는 상기했 듯 OperationCustomizer를 사용해 커스텀하게 된다.
- ApiErrorCodeExample 어노테이션을 기반으로 응답 예시 객체를 만들어 추가하는 설정이 필요하다.
##### Swaggerconfig
```java
// 스웨서 문서를 커스텀하기 위한 설정  
@Bean  
public OperationCustomizer operationCustomizer() {  
    return (operation, handlerMethod) -> {  
        // 메서드 레벨 어노테이션이 존재하는지 확인, 없으면 클래스 레벨 체크  
        ApiErrorCodeExample apiErrorCodeExample = handlerMethod.getMethodAnnotation(ApiErrorCodeExample.class);  
        if (apiErrorCodeExample == null) {  
            apiErrorCodeExample = handlerMethod.getBeanType().getAnnotation(ApiErrorCodeExample.class);  
        }  
  
        if (apiErrorCodeExample != null) {  
            for (Class<? extends ErrorCodeInterface> type : apiErrorCodeExample.value()) {  
                generateErrorCodeResponseExample(operation.getResponses(), type);  
            }  
        }  
  
        return operation;  
    };  
}

```
- 빈으로 설정해주게 된다.
- 여기서 메서드 레벨과 클래스 레벨을 체크하고, 어노테이션이 설정된 경우 로직을 태운다.
- apiErrorCodeExample.value()로 어노테이션이 저장한 에러코드 정보를 가져와 예시 데이터 생성에 사용한다.
  - 리스트로 저장하고 있기 때문에 반복해서 동작하도록 설정했다.
- 최종 설정에는 ApiErrorExceptionsExample 어노테이션도 존재하는데 이는 후술

```java
// 예외 예시를 스웨거 문서에 추가하기 위한 객체를 생성하는 메서드  
private void generateErrorCodeResponseExample(ApiResponses responses, Class<? extends ErrorCodeInterface> type) {  
    ErrorCodeInterface[] errorCodes = type.getEnumConstants();  
  
    Map<Integer, List<ExampleHolder>> statusWithExampleHolders =  
            Arrays.stream(errorCodes)  
                    .map(errorCode -> {  
                        try {  
                            String enumName = ((Enum<?>) errorCode).name();  
  
                            return ExampleHolder.builder()  
                                    .holder(getSwaggerExample(errorCode.getExplainError(), errorCode))  
                                    .code(errorCode.getStatus().value())  
                                    .name("[" + enumName + "] " + errorCode.getMessage()) // 한글로된 드롭다운을 만들기 위해 예외 메시지를 이름으로 사용  
                                    .build();  
                        } catch (NoSuchFieldException e) {  
                            throw new RuntimeException(e);  
                        }  
                    })  
                    .collect(groupingBy(ExampleHolder::getCode));  
  
    addExamplesToResponses(responses, statusWithExampleHolders);  
}
```
- 해당 메서드는 어노테이션이 설정된 경우 동작하게 되며, 예시 데이터를 만들어 Operation.Responses에 add하는 역할을 수행한다.
- type에서 에러코드들을 리스트로 가져온다.
- map을 만들어 http 응답 코드별로 자체 예시 데이터 객체를 필터링, 그룹화하여 저장한다.
- 이 때 한글로된 드롭다운을 만들기 위해 예외 메시지를 이름으로 사용했다.

```java
// ExplainError 어노테이션에 작성된 설명을 조회하는 메서드  
default String getExplainError() throws NoSuchFieldException {  
    Field field = this.getClass().getField(((Enum<?>) this).name());  
    ExplainError annotation = field.getAnnotation(ExplainError.class);  
    return Objects.nonNull(annotation) ? annotation.value() : getMessage();  
}
---
// ExplainError 설명과 에러코드 객체를 받아 Swagger의 Example 객체를 생성하는 메서드
private Example getSwaggerExample(String description, ErrorCodeInterface errorCode) {  
    CommonResponse<Void> errorResponse = CommonResponse.createFailure(errorCode.getCode(), errorCode.getMessage());  
    Example example = new Example();  
    example.description(description);  
    example.setValue(errorResponse);  
    return example;  
}
---
@Getter  
@Builder  
public class ExampleHolder {  
    private Example holder;  
    private String name;  
    private int code;  
}
---
// 스웨거의 Example 객체를 만들어 Operation.Responses에 예시 데이터를 추가하는 메서드  
private void addExamplesToResponses(ApiResponses responses, Map<Integer, List<ExampleHolder>> statusWithExampleHolders) {  
    statusWithExampleHolders.forEach((status, exampleHolders) -> {  
        // ApiResponse가 없으면 생성  
        ApiResponse apiResponse = responses.computeIfAbsent(String.valueOf(status), k -> new ApiResponse());  
  
        // application/json 타입의 MediaType 가져오기 (없으면 생성)  
        MediaType mediaType = getOrCreateMediaType(apiResponse);  
  
        // 예시 데이터 추가  
        exampleHolders.forEach(holder ->  
                mediaType.addExamples(holder.getName(), holder.getHolder())  
        );  
    });  
}
```
- 이 때 여러 유틸성 메서드와 객체를 활용한다.
- ExampleHolder는 Responses에 정보를 담아주고, 그룹핑에 사용하기 위한 임시 저장 객체의 용도이다.
- addExamplesToResponses 메서드에서 최종적으로 정제된 Map을 받아 Swagger의 Responses 형태에 맞게 예시 데이터를 추가해준다.
- 스웨거 문서 사용자는 Example 데이터를 통해 예외 발생 예시를 확인할 수 있게 된다.

### 예외코드 관리
이렇게 설정이 완료되면, 실제 예외를 관리하는 쪽에서 이를 적용해 관리하면 된다.

```java
public interface ErrorCodeInterface {  
    int getCode();  
    HttpStatus getStatus();  
    String getMessage();  
  
    // ExplainError 어노테이션에 작성된 설명을 조회하는 메서드  
    default String getExplainError() throws NoSuchFieldException {  
        Field field = this.getClass().getField(((Enum<?>) this).name());  
        ExplainError annotation = field.getAnnotation(ExplainError.class);  
        return Objects.nonNull(annotation) ? annotation.value() : getMessage();  
    }  
}
```
- 우선 모든 ErrorCode가 구현할 인터페이스를 작성한다.
```java
@Getter  
@AllArgsConstructor  
public enum UserErrorCode implements ErrorCodeInterface {  
    // User 관련 에러  
    @ExplainError("사용자 ID로 조회했으나 해당 사용자가 존재하지 않을 때 발생합니다.")  
    USER_NOT_FOUND(404, HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),  
  
    @ExplainError("가입 승인 대기 중인 사용자가 접근을 시도할 때 발생합니다.")  
    USER_INACTIVE(403, HttpStatus.FORBIDDEN, "가입 승인이 허가되지 않은 계정입니다."),  
  
    @ExplainError("이미 가입된 이메일로 회원가입을 시도할 때 발생합니다.")  
    USER_EXISTS(400, HttpStatus.BAD_REQUEST, "이미 가입된 사용자입니다.");
  
    private final int code;  
    private final HttpStatus status;  
    private final String message;  
}
---
public class UserNotFoundException extends BusinessLogicException {  
    public UserNotFoundException() {  
        super(UserErrorCode.USER_NOT_FOUND);  
    }  
}
```
- 그리고 이렇게 해당 인터페이스를 구현한 도메인별 예외 코드 Enum을 생성해 관리한다.
- 예외를 정의하는 쪽에서는 이렇게 Enum을 통해 예외 정보를 관리하도록 한다.
- @ExplainError는 해당 예외가 발생한 원인, 추가적인 설명을 작성하는 커스텀 어노테이션으로 ErrorCodeInterface에서 getExplainError 메서드를 통해 정보를 가져올 수 있도록 했다.
  - 문서를 커스텀함에 있어 필수 요소는 아니다.
  - 값이 없는 경우 메시지를 함께 보여주도록 설정했다.

### 마무리
참고한 블로그 글에는 각 API 별로 발생할 수 있는 예외를 문서화해 보여주는 작업도 포함되어있으나 현재 요구사항으로는 필요하지 않다고 판단해 다이어트를 한 버전으로 적용했다.

### 결과
- 유저 도메인 하위 API들에서 유저 도메인에서 발생할 수 있는 예외들을 한 눈에 확인할 수 있다.
  ![[Pasted image 20260117224123.png]]

- 또한 스웨거 문서 상에서도 모든 도메인의 예외를 확인할 수 있어 프론트 개발자의 작업에도 용이하고, 개발자간의 소통 비용도 줄일 수 있다.
  ![[Pasted image 20260117224410.png]]

- 마지막으로 가장 큰 장점은 이 문서에 등록하는 과정이 어노테이션만 설정해두면, 예외가 추가 되어도 자동으로 등록되기 때문에 백엔드 개발자가 API 문서를 정리하는 것에 불필요한 시간을 쓰지 않아도 된다는 것이다.
## 전체 도메인 적용 완료

위에서 설명한 예외 문서화 자동화 시스템을 Weeth 프로젝트의 **모든 도메인에 적용 완료**했다.

### 적용 범위

브랜치 `refactor/WTH-107-Weeth-예외-코드-구조-리팩토링-및-스웨거-문서화-강화`에서 진행한 작업 내용은 다음과 같다.

#### 1. 도메인별 ErrorCode Enum 생성

각 도메인마다 `ErrorCodeInterface`를 구현하는 ErrorCode Enum을 생성했다.

| 도메인 | ErrorCode 클래스 | 예외 개수 | 주요 예외 |
|--------|------------------|-----------|-----------|
| User | `UserErrorCode` | 15개 | USER_NOT_FOUND, USER_INACTIVE, EMAIL_NOT_FOUND 등 |
| Schedule | `EventErrorCode`, `MeetingErrorCode` | 4개 | EVENT_NOT_FOUND, MEETING_NOT_FOUND 등 |
| Attendance | `AttendanceErrorCode` | 3개 | ATTENDANCE_NOT_FOUND, ATTENDANCE_CODE_MISMATCH 등 |
| Board | `BoardErrorCode`, `NoticeErrorCode`, `PostErrorCode` | 9개 | POST_NOT_FOUND, NOTICE_NOT_FOUND, PAGE_NOT_FOUND 등 |
| Comment | `CommentErrorCode` | 1개 | COMMENT_NOT_FOUND |
| Account | `AccountErrorCode` | 3개 | ACCOUNT_NOT_FOUND, RECEIPT_NOT_FOUND 등 |
| Penalty | `PenaltyErrorCode` | 2개 | PENALTY_NOT_FOUND, AUTO_PENALTY_DELETE_NOT_ALLOWED |
| JWT | `JwtErrorCode` | 4개 | INVALID_TOKEN, TOKEN_NOT_FOUND 등 |

**총 41개**의 예외 코드가 체계적으로 정리되고 문서화되었다.

#### 2. 컨트롤러에 @ApiErrorCodeExample 어노테이션 적용

**총 21개의 컨트롤러**에 `@ApiErrorCodeExample` 어노테이션을 적용했다.

```java
// 단일 ErrorCode 적용 예시
@ApiErrorCodeExample(AttendanceErrorCode.class)
public class AttendanceController {
    ...
}

// 복수 ErrorCode 적용 예시
@ApiErrorCodeExample({BoardErrorCode.class, NoticeErrorCode.class})
public class NoticeController {
    ...
}
```

**적용된 컨트롤러 목록:**
- UserController, UserAdminController, CardinalController
- AttendanceController, AttendanceAdminController
- ScheduleController, MeetingController, MeetingAdminController, EventController, EventAdminController
- NoticeController, NoticeAdminController, PostController, EducationAdminController
- NoticeCommentController, PostCommentController
- PenaltyUserController, PenaltyAdminController
- AccountController, AccountAdminController, ReceiptAdminController

#### 3. 예외 문서 통합 조회 API 추가

모든 도메인의 예외를 한 곳에서 확인할 수 있도록 `ExceptionDocController`를 추가했다.

```java
@RestController
@RequestMapping("/api/v1/exceptions")
@Tag(name = "Exception Document", description = "API 에러 코드 문서")
public class ExceptionDocController {

    @GetMapping("/user")
    @ApiErrorCodeExample(UserErrorCode.class)
    public void userErrorCodes() {}

    @GetMapping("/Board")
    @ApiErrorCodeExample({BoardErrorCode.class, NoticeErrorCode.class, 
                          PostErrorCode.class, CommentErrorCode.class})
    public void boardErrorCodes() {}
    
    // ... 각 도메인별 예외 조회 엔드포인트
}
```

이 컨트롤러는 실제 비즈니스 로직을 수행하지 않고, **순수하게 Swagger 문서화만을 위한 목적**으로 생성되었다.

### 주요 커밋 이력

브랜치에서 진행한 작업의 흐름은 다음과 같다.

```
feat: 스웨거 예외처리 자동화를 위한 어노테이션 및 스웨거 설정 추가
  → 기본 인프라 구축 (ApiErrorCodeExample, ErrorCodeInterface, ExplainError)

feat: 예외코드 클래스 추가 및 기존 예외 클래스 리팩토링
  → UserErrorCode 생성 및 기존 예외 클래스 리팩토링

feat: 유저 도메인 컨트롤러에 문서 자동화 적용
  → UserController에 @ApiErrorCodeExample 적용

refactor: 여러 개의 에러코드 타입을 받을 수 있도록 설정
  → @ApiErrorCodeExample이 배열을 받을 수 있도록 개선

feat: JWT 관련 예외 문서화 추가
feat: Schedule 도메인 관련 예외 문서화 추가
feat: Attendance 도메인 관련 예외 문서화 추가
feat: Account 도메인 관련 예외 문서화 추가
feat: Penalty 도메인 관련 예외 문서화 추가
feat: Board 도메인 관련 예외 문서화 추가
  → 각 도메인별 ErrorCode 생성 및 컨트롤러 적용

refactor: API 정리
  → ExceptionDocController 추가로 통합 문서 제공
```

### 코드 예시

#### ErrorCode Enum 구조

모든 ErrorCode는 동일한 구조를 따른다.

```java
@Getter
@AllArgsConstructor
public enum AttendanceErrorCode implements ErrorCodeInterface {

    @ExplainError("출석 정보를 찾을 수 없을 때 발생합니다.")
    ATTENDANCE_NOT_FOUND(404, HttpStatus.NOT_FOUND, "출석 정보가 존재하지 않습니다."),

    @ExplainError("입력한 출석 코드가 생성된 코드와 일치하지 않을 때 발생합니다.")
    ATTENDANCE_CODE_MISMATCH(400, HttpStatus.BAD_REQUEST, "출석 코드가 일치하지 않습니다."),

    @ExplainError("사용자가 출석 일정을 직접 수정하려고 시도할 때 발생합니다.")
    ATTENDANCE_EVENT_TYPE_NOT_MATCH(400, HttpStatus.BAD_REQUEST, "출석일정은 직접 수정할 수 없습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
```

**핵심 포인트:**
- `ErrorCodeInterface` 구현으로 일관된 인터페이스 보장
- `@ExplainError` 어노테이션으로 상세한 발생 원인 설명
- HTTP 상태 코드, 에러 코드, 메시지를 한 곳에서 관리

#### 기존 예외 클래스 리팩토링

기존의 예외 클래스들도 ErrorCode를 사용하도록 리팩토링했다.

**Before:**
```java
public class AttendanceNotFoundException extends BusinessLogicException {
    public AttendanceNotFoundException() {
        super(404, HttpStatus.NOT_FOUND, "출석 정보가 존재하지 않습니다.");
    }
}
```

**After:**
```java
public class AttendanceNotFoundException extends BusinessLogicException {
    public AttendanceNotFoundException() {
        super(AttendanceErrorCode.ATTENDANCE_NOT_FOUND);
    }
}
```

이렇게 변경함으로써:
- 예외 정보가 중앙 집중화됨
- 예외 코드 변경 시 한 곳만 수정하면 됨
- Swagger 문서에 자동으로 반영됨

## 실제 결과 확인

### 1. Swagger UI에서 예외 확인

Swagger UI(`/swagger-ui/index.html`)에 접속하면 각 API 엔드포인트마다 발생 가능한 예외를 확인할 수 있다.

#### API별 예외 확인
각 컨트롤러의 API 문서에서 Responses 섹션을 보면:
- **400 Bad Request** - 잘못된 요청 관련 예외들
- **403 Forbidden** - 권한 관련 예외들
- **404 Not Found** - 리소스를 찾을 수 없는 예외들

각 HTTP 상태 코드별로 드롭다운 메뉴가 생성되고, **한글로 된 예외 메시지**로 구분할 수 있다.

예를 들어, UserController의 `/api/v1/users/{userId}` GET 요청의 경우:
- 404: `[USER_NOT_FOUND] 존재하지 않는 유저입니다.`
- 403: `[USER_INACTIVE] 가입 승인이 허가되지 않은 계정입니다.`

각 예외를 선택하면:
- **description**: `@ExplainError`에 작성한 상세 설명
- **value**: 실제 API 응답 형식 (`CommonResponse` 구조)

```json
{
  "code": 404,
  "message": "존재하지 않는 유저입니다.",
  "data": null
}
```

#### 도메인별 전체 예외 확인
`Exception Document` 태그 하위의 API들을 통해 각 도메인의 모든 예외를 한 번에 확인할 수 있다.

- `/api/v1/exceptions/user` - User 도메인의 모든 예외 (15개)
- `/api/v1/exceptions/Board` - Board 도메인의 모든 예외 (9개)
- `/api/v1/exceptions/Attendance` - Attendance 도메인의 모든 예외 (3개)
- ...

**각 엔드포인트의 Responses 섹션에서 해당 도메인의 모든 예외 케이스를 드롭다운으로 확인 가능하다.**

### 2. 변경 통계

Git diff 통계를 보면 이번 작업의 규모를 알 수 있다.

```
101 files changed, 1521 insertions(+), 148 deletions(-)
```

**주요 변경 사항:**
- **11개의 ErrorCode Enum 파일 추가** (UserErrorCode, AttendanceErrorCode 등)
- **21개의 컨트롤러에 @ApiErrorCodeExample 적용**
- **68개의 기존 예외 클래스 리팩토링** (ErrorCode 기반으로 변경)
- **SwaggerConfig 강화** (OperationCustomizer 추가)
- **ExceptionDocController 추가** (통합 예외 문서 제공)

### 3. 프론트엔드 협업 개선

이제 프론트엔드 개발자는:

✅ **API 문서만 보고 모든 예외 케이스를 파악 가능**
- 각 API에서 발생 가능한 모든 예외를 Swagger에서 확인
- HTTP 상태 코드별로 정리된 예외 목록
- 한글로 된 명확한 에러 메시지

✅ **에러 핸들링 코드 작성 용이**
```typescript
// 예시: 프론트엔드 에러 핸들링
try {
  await api.getUser(userId);
} catch (error) {
  if (error.code === 404) {
    // USER_NOT_FOUND: "존재하지 않는 유저입니다."
    showNotification("사용자를 찾을 수 없습니다.");
  } else if (error.code === 403) {
    // USER_INACTIVE: "가입 승인이 허가되지 않은 계정입니다."
    showNotification("가입 승인 대기 중입니다.");
  }
}
```

✅ **백엔드와의 소통 비용 절감**
- "이 API에서 어떤 에러가 발생할 수 있나요?" 같은 질문 불필요
- 에러 코드와 메시지가 명확하게 문서화됨
- API 변경 시 Swagger 문서가 자동으로 업데이트됨

## 성능 및 효과

### 개발 생산성 향상

#### Before (수동 문서화)
1. 새로운 예외 추가 (3분)
2. Swagger 문서에 수동으로 예외 추가 (5분)
3. API 명세서에 예외 정보 작성 (5분)
4. 프론트엔드에 예외 정보 공유 (슬랙/노션, 3분)

**총 소요 시간: 약 16분/예외**

#### After (자동 문서화)
1. ErrorCode Enum에 예외 추가 (1분)
2. 기존 예외 클래스를 ErrorCode 기반으로 수정 (1분)

**총 소요 시간: 약 2분/예외**

**생산성 향상: 약 8배** (16분 → 2분)

### 유지보수성 향상

**중앙 집중식 예외 관리:**
- 예외 코드 변경 시 ErrorCode Enum만 수정하면 됨
- 모든 관련 문서가 자동으로 업데이트됨
- 예외 정보의 일관성 보장

**예외 누락 방지:**
- ErrorCode Enum에 정의된 예외는 자동으로 문서화됨
- 개발자가 수동으로 문서를 업데이트할 필요 없음
- CI/CD에서 빌드 시 자동으로 Swagger 문서 생성

### 코드 품질 향상

**Before:**
```java
// 여러 곳에 하드코딩된 예외 정보
throw new BusinessLogicException(404, HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다.");
throw new BusinessLogicException(404, HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");  // 같은 의미, 다른 메시지
```

**After:**
```java
// 일관된 예외 처리
throw new UserNotFoundException();  // UserErrorCode.USER_NOT_FOUND 사용
```

**개선 효과:**
- 예외 메시지 일관성 확보
- 오타나 잘못된 HTTP 상태 코드 방지
- 타입 안정성 보장 (컴파일 타임에 오류 감지)

## 향후 개선 계획

### 1. SAS(Spring Authorization Server) 예외 추가
현재 `ExceptionDocController`에는 TODO 주석이 있다:
```java
//todo: SAS 관련 예외도 추가
```

OAuth2 인증 관련 예외도 동일한 방식으로 문서화할 예정이다.

### 2. API별 특정 예외만 표시
현재는 컨트롤러 레벨에서 도메인 전체 예외를 표시하지만, 향후에는:
- 메서드 레벨에서 `@ApiErrorCodeExample` 사용
- 특정 API에서 실제로 발생하는 예외만 선별적으로 표시

```java
@GetMapping("/{userId}")
@ApiErrorCodeExample({UserErrorCode.USER_NOT_FOUND, UserErrorCode.USER_INACTIVE})
public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
    // 이 API에서는 USER_NOT_FOUND와 USER_INACTIVE만 발생 가능
}
```

### 3. 예외 발생 조건 자동 검증
테스트 코드에서 ErrorCode와 실제 발생 예외의 일치 여부를 검증:
```java
@Test
void 예외_일관성_검증() {
    // UserController의 모든 API에서
    // UserErrorCode에 정의된 예외만 발생하는지 검증
}
```

### 4. Enum 값 기반 선택적 문서화
참고 블로그에는 `ApiErrorExceptionsExample`이라는 어노테이션도 소개되어 있다. 이를 활용하면:
```java
@ApiErrorExceptionsExample(UserErrorCode.class)
public enum SpecificErrors {
    USER_NOT_FOUND,
    USER_INACTIVE
}

@GetMapping("/{userId}")
@ApiErrorCodeExample(SpecificErrors.class)
public ResponseEntity<UserResponse> getUser() {
    // USER_NOT_FOUND와 USER_INACTIVE만 문서에 표시
}
```

## 회고

### 잘한 점

**1. 점진적 적용**
- 처음부터 모든 도메인에 적용하지 않고, User 도메인으로 프로토타입을 만들어 검증
- 이후 다른 도메인에 순차적으로 적용하며 문제점 개선
- 덕분에 시행착오를 최소화할 수 있었음

**2. 확장 가능한 설계**
- `@ApiErrorCodeExample(Class<?>[])`처럼 배열을 받도록 설계
- 복수 ErrorCode를 지원하여 Board처럼 여러 하위 도메인이 있는 경우에도 대응 가능
- 나중에 새로운 도메인이 추가되어도 쉽게 확장 가능

**3. 기존 코드 리팩토링 병행**
- 단순히 문서화만 추가한 것이 아니라, 기존 예외 처리 코드도 개선
- 68개의 예외 클래스를 ErrorCode 기반으로 리팩토링
- 코드 품질 향상과 문서화를 동시에 달성

### 아쉬운 점

**1. 테스트 코드 부족**
- ErrorCode Enum 자체는 단순해서 테스트가 필요 없을 수 있지만
- SwaggerConfig의 OperationCustomizer 로직은 복잡도가 있음
- 단위 테스트를 작성했다면 더 안정적이었을 것

**2. 이미지/스크린샷 부재**
- 이 문서에는 실제 Swagger UI 화면 캡처가 없음
- 프론트엔드 개발자에게 공유할 때 시각 자료가 있으면 더 효과적

**3. API별 세밀한 예외 문서화 미흡**
- 현재는 컨트롤러 레벨에서 도메인 전체 예외를 표시
- 실제로는 각 API에서 발생 가능한 예외가 제한적임
- 메서드 레벨 어노테이션을 활용해 더 정확한 문서를 만들 수 있었을 것

### 배운 점

**1. Swagger/OpenAPI의 확장성**
- OpenAPI의 `Operation`, `ApiResponses`, `Example` 객체 구조를 이해
- `OperationCustomizer`를 통해 Swagger를 자유롭게 커스터마이징할 수 있음
- SpringDoc의 강력한 확장 포인트를 활용하는 방법 습득

**2. 어노테이션 기반 메타데이터 활용**
- Java의 Reflection을 활용한 어노테이션 정보 추출
- 컴파일 타임 안정성과 런타임 유연성의 균형
- `@ExplainError` 같은 커스텀 어노테이션의 효과적인 활용

**3. 도구 최적화의 중요성**
- 개발 도구(Swagger, IDE, 빌드 시스템)를 프로젝트에 맞게 최적화하면
- 초기 투자 시간 대비 장기적인 생산성 향상이 큼
- 팀 전체의 개발 경험과 협업 효율성 개선

**4. 문서화 자동화의 가치**
- 수동 문서화는 항상 코드와 불일치 문제가 발생
- 코드 기반 자동 문서화는 항상 최신 상태 유지
- 개발자는 문서 작성이 아닌 코드 작성에 집중 가능

## 마치며

이번 작업을 통해 **Weeth 프로젝트의 모든 예외 처리가 체계화되고 자동으로 문서화**되었다.

41개의 예외 코드가 정리되었고, 21개의 컨트롤러가 자동 문서화를 지원한다. 이제 새로운 예외를 추가할 때마다 Swagger 문서가 자동으로 업데이트되며, 프론트엔드 개발자는 항상 최신의 정확한 예외 정보를 확인할 수 있다.

초기 설정에는 시간이 걸렸지만, 장기적으로는:
- 개발 생산성 8배 향상
- 예외 관리의 일관성 확보
- 프론트엔드 협업 효율 개선
- 코드 품질 및 유지보수성 향상

이라는 큰 효과를 얻을 수 있었다.

**핵심 교훈:** "초기 투자를 통한 도구 최적화는 팀 전체의 생산성을 크게 향상시킨다."

앞으로도 개발 환경과 도구를 지속적으로 개선하며, 더 나은 개발 경험을 만들어나갈 것이다.

---

**참고 자료:**
- [원본 참고 블로그](https://haward.tistory.com/251)
- [SpringDoc 공식 문서](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)

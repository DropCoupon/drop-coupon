package io.dropcoupon.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link GlobalExceptionHandler}의 검증 예외·일반 예외 처리 및 {@link ErrorResponse} 포맷을 검증한다. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** BindException 시 400, VALIDATION_ERROR, path/requestId/timestamp가 응답에 포함되는지 검증한다. */
    @Test
    @DisplayName("검증 예외는 VALIDATION_ERROR 코드와 400 상태로 처리된다")
    void handleValidationException() {
        MDC.put("requestId", "req-validation");
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("/test/validation");

        BindException ex = new BindException(new Object(), "target");

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).isEqualTo("요청 값이 올바르지 않습니다.");
        assertThat(body.path()).isEqualTo("/test/validation");
        assertThat(body.requestId()).isEqualTo("req-validation");
        assertThat(body.timestamp()).isNotNull();

        MDC.clear();
    }

    /** RuntimeException 시 500, INTERNAL_ERROR, path/requestId/timestamp가 응답에 포함되는지 검증한다. */
    @Test
    @DisplayName("일반 예외는 INTERNAL_ERROR 코드와 500 상태로 처리된다")
    void handleGenericException() {
        MDC.put("requestId", "req-internal");
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("/test/internal");

        RuntimeException ex = new RuntimeException("boom");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.message()).isEqualTo("일시적인 오류가 발생했습니다.");
        assertThat(body.path()).isEqualTo("/test/internal");
        assertThat(body.requestId()).isEqualTo("req-internal");
        assertThat(body.timestamp()).isNotNull();

        MDC.clear();
    }
}


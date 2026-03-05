package io.dropcoupon.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * 전역 예외를 표준 {@link ErrorResponse} 포맷으로 변환해 JSON으로 반환한다.
 * MDC의 requestId를 응답에 포함해 추적 가능하게 한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 검증 예외({@link MethodArgumentNotValidException}, {@link BindException})를 400과 VALIDATION_ERROR로 처리한다.
     *
     * @param ex      검증 예외 (본문에서는 사용하지 않으나 시그니처 통일용)
     * @param request 현재 요청 (path 추출용)
     * @return 400 Bad Request + ErrorResponse
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(Exception ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                "VALIDATION_ERROR",
                "요청 값이 올바르지 않습니다.",
                request.getRequestURI(),
                MDC.get("requestId"),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 그 외 모든 예외를 500과 INTERNAL_ERROR로 처리한다.
     *
     * @param ex      발생한 예외 (로깅용으로만 사용 가능, 본문에서는 사용하지 않음)
     * @param request 현재 요청 (path 추출용)
     * @return 500 Internal Server Error + ErrorResponse
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                "INTERNAL_ERROR",
                "일시적인 오류가 발생했습니다.",
                request.getRequestURI(),
                MDC.get("requestId"),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}


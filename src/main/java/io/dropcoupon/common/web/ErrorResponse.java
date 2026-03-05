package io.dropcoupon.common.web;

import java.time.Instant;

/**
 * API 오류 시 반환하는 표준 JSON 본문 형식.
 *
 * @param code      오류 코드 (예: VALIDATION_ERROR, INTERNAL_ERROR)
 * @param message   사용자/클라이언트용 메시지
 * @param path      요청 URI
 * @param requestId 요청 추적용 ID (MDC에서 전달)
 * @param timestamp 오류 발생 시각 (UTC)
 */
public record ErrorResponse(
        String code,
        String message,
        String path,
        String requestId,
        Instant timestamp
) {
}


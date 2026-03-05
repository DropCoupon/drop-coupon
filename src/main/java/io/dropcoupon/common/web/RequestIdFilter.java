package io.dropcoupon.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청마다 requestId를 생성·전파하고 MDC에 넣어 로그/멱등 처리에 사용할 수 있게 한다.
 * 클라이언트가 {@value #HEADER_REQUEST_ID} 헤더를 보내면 그 값을 사용하고, 없으면 UUID를 생성한다.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    /** HTTP 요청/응답에서 requestId를 주고받을 때 사용하는 헤더 이름. */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    /** MDC에 requestId를 넣을 때 사용하는 키 이름. 로그 패턴에서 %X{requestId}로 참조한다. */
    public static final String MDC_REQUEST_ID = "requestId";

    /**
     * 요청에서 requestId를 결정하고 MDC·응답 헤더에 설정한 뒤 체인을 실행한다. 종료 후 MDC에서 제거한다.
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 다음 필터/서블릿 체인
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = resolveOrGenerateRequestId(request);
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    /**
     * 요청 헤더에 {@value #HEADER_REQUEST_ID}가 있으면 그 값을, 없으면 새 UUID를 반환한다.
     *
     * @param request HTTP 요청
     * @return 사용할 requestId (null이 아님)
     */
    private String resolveOrGenerateRequestId(HttpServletRequest request) {
        String headerValue = request.getHeader(HEADER_REQUEST_ID);
        if (StringUtils.hasText(headerValue)) {
            return headerValue;
        }
        return UUID.randomUUID().toString();
    }
}


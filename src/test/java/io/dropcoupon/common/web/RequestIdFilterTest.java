package io.dropcoupon.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link RequestIdFilter}의 requestId 생성·전파·MDC 정리 동작을 검증한다. */
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    /** 헤더 없을 때 새 requestId가 생성되어 응답 헤더에 설정되는지 검증한다. */
    @Test
    @DisplayName("X-Request-Id 헤더가 없으면 새 requestId를 생성해 헤더와 MDC에 넣는다")
    void generatesRequestIdWhenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, noopChain());

        String headerValue = response.getHeader(RequestIdFilter.HEADER_REQUEST_ID);
        assertThat(headerValue).isNotBlank();
    }

    /** 헤더에 값이 있으면 그 값이 응답 헤더에 그대로 반영되는지 검증한다. */
    @Test
    @DisplayName("X-Request-Id 헤더가 있으면 해당 값을 그대로 사용한다")
    void usesExistingRequestIdHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_REQUEST_ID, "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, noopChain());

        String headerValue = response.getHeader(RequestIdFilter.HEADER_REQUEST_ID);
        assertThat(headerValue).isEqualTo("req-123");
    }

    /** 필터 체인 종료 후 MDC에서 requestId가 제거되는지 검증한다. */
    @Test
    @DisplayName("필터 실행 후에는 MDC에서 requestId를 제거한다")
    void clearsMdcAfterFilter() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            // 체인 내부에서는 MDC에 requestId가 있어야 한다
            assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID)).isNotBlank();
        });

        // 필터 종료 후에는 MDC에서 제거되어야 한다
        assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID)).isNull();
    }

    /** 다음 필터/서블릿을 호출하지 않는 no-op 체인을 반환한다. */
    private FilterChain noopChain() {
        return (ServletRequest request, ServletResponse response) -> {
            // no-op
        };
    }
}


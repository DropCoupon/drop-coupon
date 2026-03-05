package io.dropcoupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 대용량 트래픽 대응 선착순 쿠폰 발급 애플리케이션 진입점.
 */
@SpringBootApplication
public class CouponApplication {

    /**
     * Spring Boot 애플리케이션을 기동한다.
     *
     * @param args 커맨드 라인 인자 (예: --spring.profiles.active=local)
     */
    public static void main(String[] args) {
        SpringApplication.run(CouponApplication.class, args);
    }
}


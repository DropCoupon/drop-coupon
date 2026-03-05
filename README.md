# drop-coupon

대용량 트래픽 대응 선착순 쿠폰 발급 시스템 (Spring Boot 3.5, Java 25).

## 로컬 실행 방법

1. 인프라 기동:
   ```bash
   docker-compose up -d
   ```

2. 애플리케이션 실행:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

3. Health 확인:
   ```bash
   curl -s http://localhost:8080/actuator/health | jq
   ```

Flyway는 앱 기동 시 자동 실행되며, `src/main/resources/db/migration` 에 있는 마이그레이션이 적용됩니다.

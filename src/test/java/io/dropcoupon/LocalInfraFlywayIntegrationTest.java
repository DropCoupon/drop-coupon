package io.dropcoupon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Postgres(Testcontainers) + Flyway 환경에서 애플리케이션 컨텍스트 기동을 검증한다.
 * Docker가 없으면 테스트는 스킵된다 ({@code disabledWithoutDocker = true}).
 */
@SpringBootTest(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
    }
)
@Testcontainers(disabledWithoutDocker = true)
class LocalInfraFlywayIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("dropcoupon")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    /** Postgres 컨테이너가 기동된 상태에서 컨텍스트 로드 및 Flyway 실행이 되는지 검증한다. */
    @Test
    @DisplayName("Postgres + Flyway 환경에서 애플리케이션 컨텍스트가 기동하고 Flyway가 실행된다")
    void contextLoadsWithPostgresAndFlyway() {
        assertThat(postgres.isRunning()).isTrue();
    }
}

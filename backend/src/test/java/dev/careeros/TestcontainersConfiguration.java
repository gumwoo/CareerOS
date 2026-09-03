package dev.careeros;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/**
	 * docker-compose.yml 과 같은 이미지를 쓴다.
	 *
	 * <p>기본값인 {@code postgres:latest} 로는 V1 마이그레이션의
	 * {@code create extension vector} 가 실패한다. 테스트와 운영이 다른 스키마 위에서
	 * 돌면 마이그레이션 검증이 의미를 잃으므로 이미지를 맞춘다. (ADR-0005)
	 */
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(
				DockerImageName.parse("pgvector/pgvector:pg16")
						.asCompatibleSubstituteFor("postgres"));
	}

}

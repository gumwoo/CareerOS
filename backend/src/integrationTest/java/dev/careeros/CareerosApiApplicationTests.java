package dev.careeros;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
/**
 * 기본 프로파일에는 아직 EvidenceExtractor 구현이 없다.
 * 실제 LLM 추출기를 붙이면 이 프로파일 지정은 빠진다.
 */
@SpringBootTest
@ActiveProfiles("stub")
class CareerosApiApplicationTests {

	@Test
	void contextLoads() {
	}

}

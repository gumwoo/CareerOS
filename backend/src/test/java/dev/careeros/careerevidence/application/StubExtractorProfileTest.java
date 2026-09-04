package dev.careeros.careerevidence.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스텁이 사실을 만들어내는 경로를 막았는지 확인한다.
 *
 * <p>이전에는 {@code @Profile("!llm")}이라 프로파일을 지정하지 않으면 스텁이 기본값이었다.
 * 운영에서 프로파일을 빠뜨리면 {@code [STUB] not extracted}가 DRAFT로 저장되고,
 * 사용자가 확인하면 CONFIRMED 사실이 된다. {@code confirm()}은 스텁인지 보지 않는다.
 *
 * <p>지금은 {@code stub} 프로파일에서만 등록되므로 기본 프로파일에는
 * {@code EvidenceExtractor} 빈이 아예 없고 기동이 실패한다.
 * 누군가 이 조건을 되돌리면 이 테스트가 잡는다.
 */
class StubExtractorProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StubEvidenceExtractor.class);

    @Test
    @DisplayName("기본 프로파일에는 스텁이 등록되지 않는다")
    void stubIsAbsentByDefault() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(EvidenceExtractor.class));
    }

    @Test
    @DisplayName("stub 프로파일을 명시해야만 등록된다")
    void stubRequiresExplicitProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=stub")
                .run(context -> assertThat(context).hasSingleBean(EvidenceExtractor.class));
    }
}

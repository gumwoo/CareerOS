package dev.careeros.careerevidence.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CareerEvidenceTest {

    private static final String RAW_TEXT =
            "다중 SSE 연결에서 최대 14초 지연이 발생했고, JFR로 분석해 락 경합을 찾았습니다.";
    private static final String EXCERPT = "다중 SSE 연결에서 최대 14초 지연이 발생했고";

    private final SourceInput sourceInput = SourceInput.create(SourceType.USER_INPUT, RAW_TEXT, null);

    private CareerEvidence.Builder valid() {
        return CareerEvidence.builder(EvidenceCode.of(1))
                .title("SSE 다중 연결 지연")
                .contextProject("ESS")
                .problem("최대 14초 지연")
                .action("전송 구조 직렬화")
                .result("정체 제거")
                .extractedBy("claude-sonnet-5", "v1")
                .source(sourceInput.getType(), sourceInput.getId(), EXCERPT, null, Instant.now());
    }

    @Test
    @DisplayName("추출 출처를 기록하지 않으면 만들 수 없다")
    void requiresExtractionOrigin() {
        CareerEvidence.Builder withoutOrigin = CareerEvidence.builder(EvidenceCode.of(1))
                .title("SSE 다중 연결 지연")
                .contextProject("ESS")
                .problem("최대 14초 지연")
                .action("전송 구조 직렬화")
                .result("정체 제거")
                .source(sourceInput.getType(), sourceInput.getId(), EXCERPT, null, Instant.now());

        assertThatThrownBy(() -> withoutOrigin.buildVerifiedAgainst(sourceInput))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("extractionModel");
    }

    @Test
    @DisplayName("무엇이 만들었는지가 Evidence 에 남는다")
    void keepsExtractionOrigin() {
        CareerEvidence evidence = valid().buildVerifiedAgainst(sourceInput);

        assertThat(evidence.getExtractionModel()).isEqualTo("claude-sonnet-5");
        assertThat(evidence.getPromptVersion()).isEqualTo("v1");
    }

    @Test
    @DisplayName("프롬프트를 쓰지 않는 추출기는 버전이 null 이다")
    void allowsNullPromptVersion() {
        CareerEvidence evidence = valid().extractedBy("stub", null).buildVerifiedAgainst(sourceInput);

        assertThat(evidence.getExtractionModel()).isEqualTo("stub");
        assertThat(evidence.getPromptVersion()).isNull();
    }

    @Test
    @DisplayName("skills 와 category 는 비어 있어도 된다 — 지어낼 압력을 주지 않는다")
    void allowsEmptySkillsAndCategories() {
        assertThatCode(() -> valid()
                .skills(Set.of())
                .categories(Set.of())
                .buildVerifiedAgainst(sourceInput)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("최초 상태는 언제나 DRAFT 이고 confirm 은 한 번만 된다")
    void startsAsDraftAndConfirmsOnce() {
        CareerEvidence evidence = valid().buildVerifiedAgainst(sourceInput);
        assertThat(evidence.getStatus()).isEqualTo(EvidenceStatus.DRAFT);

        evidence.confirm();
        assertThat(evidence.getStatus()).isEqualTo(EvidenceStatus.CONFIRMED);

        assertThatThrownBy(evidence::confirm).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("원문에서 나오지 않은 excerpt 로는 만들 수 없다 — 도메인이 원문을 요구한다")
    void requiresVerbatimExcerpt() {
        CareerEvidence.Builder fabricated = valid()
                .source(sourceInput.getType(), sourceInput.getId(),
                        "SSE 성능 문제를 분석하여 병목을 제거하였습니다", null, Instant.now());

        assertThatThrownBy(() -> fabricated.buildVerifiedAgainst(sourceInput))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verbatim");
    }
}

package dev.careeros.careerevidence.application;

import com.anthropic.models.messages.StopReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 추출 출처와 응답 완결성 — 둘 다 "모르는 것을 정상으로 취급하지 않는다"에 해당한다.
 */
class ExtractionOriginTest {

    @Test
    @DisplayName("무엇이 만들었는지 없이는 출처를 만들 수 없다")
    void requiresModel() {
        assertThatThrownBy(() -> new ExtractionOrigin(null, "v1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExtractionOrigin("  ", "v1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("프롬프트를 쓰지 않는 추출기는 버전이 null 이다")
    void allowsNullPromptVersion() {
        assertThatCode(() -> ExtractionOrigin.of("stub")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("END_TURN 만 정상 종료로 인정한다")
    void acceptsOnlyEndTurn() {
        assertThatCode(() -> AnthropicEvidenceExtractor.assertCompleted(
                StopReason.END_TURN, UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("stopReason 이 없으면 거부한다 — 모르는 것은 정상이 아니다")
    void rejectsUnknownStopReason() {
        assertThatThrownBy(() -> AnthropicEvidenceExtractor.assertCompleted(
                null, UUID.randomUUID()))
                .isInstanceOf(EvidenceExtractionException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("MAX_TOKENS / REFUSAL 은 거부한다 — REFUSAL 은 200 으로 온다")
    void rejectsIncompleteOrRefused() {
        for (StopReason reason : new StopReason[]{StopReason.MAX_TOKENS, StopReason.REFUSAL}) {
            assertThatThrownBy(() -> AnthropicEvidenceExtractor.assertCompleted(
                    reason, UUID.randomUUID()))
                    .as(reason.toString())
                    .isInstanceOf(EvidenceExtractionException.class);
        }
    }
}

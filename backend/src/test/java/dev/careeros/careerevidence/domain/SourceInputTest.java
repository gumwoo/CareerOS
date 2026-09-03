package dev.careeros.careerevidence.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * source.excerpt 원문 대조. JSON Schema가 보장하지 못하는 부분이다.
 */
class SourceInputTest {

    private static final String RAW_TEXT = """
            다중 SSE 연결에서 최대 14초 지연이 발생했고, JFR로 분석해서
            SseEmitter.send()의 락 경합을 찾아 전송 구조를 직렬화했습니다.
            """;

    @Test
    @DisplayName("원문에서 그대로 잘라낸 구절은 통과한다")
    void acceptsVerbatimExcerpt() {
        SourceInput input = SourceInput.create(SourceType.USER_INPUT, RAW_TEXT, null);

        assertThat(input.contains("다중 SSE 연결에서 최대 14초 지연이 발생했고")).isTrue();
        assertThat(input.contains("SseEmitter.send()의 락 경합을 찾아 전송 구조를")).isTrue();
    }

    @Test
    @DisplayName("줄바꿈 때문에 끊긴 구절도 공백 정규화 후 통과한다")
    void normalizesWhitespaceOnly() {
        SourceInput input = SourceInput.create(SourceType.USER_INPUT, RAW_TEXT, null);

        assertThat(input.contains("JFR로   분석해서\n\n  SseEmitter.send()")).isTrue();
    }

    @Test
    @DisplayName("요약하거나 윤문한 excerpt는 거부한다")
    void rejectsSummarizedExcerpt() {
        SourceInput input = SourceInput.create(SourceType.USER_INPUT, RAW_TEXT, null);

        // 내용은 맞지만 원문에 없는 문장 — 이런 것이 통과하면 추적 체인이 형식만 남는다.
        assertThat(input.contains("SSE 지연 문제를 분석하여 성능을 개선했습니다")).isFalse();
        assertThat(input.contains("지연 시간을 14초에서 0초로 줄이는 데 성공했습니다")).isFalse();
    }

    @Test
    @DisplayName("너무 짧은 excerpt는 거부한다 — 한두 글자는 어떤 원문에도 들어 있다")
    void rejectsTooShortExcerpt() {
        SourceInput input = SourceInput.create(SourceType.USER_INPUT, RAW_TEXT, null);

        // 아래는 전부 원문에 실제로 들어 있는 문자열이지만, 근거로 인정하지 않는다.
        assertThat(input.contains("다중")).isFalse();
        assertThat(input.contains("JFR로 분석해서")).isFalse();
        assertThat(SourceInput.MIN_EXCERPT_LENGTH).isEqualTo(20);
    }

    @Test
    @DisplayName("빈 excerpt는 거부한다")
    void rejectsBlankExcerpt() {
        SourceInput input = SourceInput.create(SourceType.USER_INPUT, RAW_TEXT, null);

        assertThat(input.contains(null)).isFalse();
        assertThat(input.contains("   ")).isFalse();
    }

    @Test
    @DisplayName("원문 없이는 생성할 수 없다")
    void requiresRawText() {
        assertThatThrownBy(() -> SourceInput.create(SourceType.USER_INPUT, "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rawText");
    }
}

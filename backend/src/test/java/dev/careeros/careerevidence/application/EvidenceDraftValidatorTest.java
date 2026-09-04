package dev.careeros.careerevidence.application;

import dev.careeros.careerevidence.domain.SourceInput;
import dev.careeros.careerevidence.domain.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * schemas/career-evidence.schema.json 이 실제로 강제되는지 확인한다.
 * 스키마가 저장소에 있는 것과 런타임에 검증되는 것은 다르다.
 */
class EvidenceDraftValidatorTest {

    private static final String RAW_TEXT =
            "다중 SSE 연결에서 최대 14초 지연이 발생했고, JFR로 분석해서 락 경합을 찾았습니다. "
                    + "JavaMonitorEnter가 341에서 0으로 줄었습니다.";

    private EvidenceDraftValidator validator;
    private SourceInput sourceInput;

    @BeforeEach
    void setUp() {
        validator = new EvidenceDraftValidator();
        sourceInput = SourceInput.create(SourceType.USER_INPUT, RAW_TEXT, null);
    }

    @Test
    @DisplayName("필수 필드를 모두 갖춘 초안은 통과한다")
    void acceptsValidDraft() {
        assertThatCode(() -> validator.validateAll(draft(), sourceInput)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sourceExcerpt가 없으면 거부한다 — 근거 구간 없이는 원문 대조가 성립하지 않는다")
    void rejectsMissingSourceExcerpt() {
        String without = draft().replaceFirst(",\\s*\"sourceExcerpt\"\\s*:\\s*\"[^\"]*\"", "");

        assertThatThrownBy(() -> validator.validateAll(without, sourceInput))
                .isInstanceOf(EvidenceExtractionException.class)
                .hasMessageContaining("llm.schema.json");
    }

    @Test
    @DisplayName("모델이 출처를 말하려 하면 거부한다 — provenance는 시스템 소유다")
    void rejectsModelClaimingProvenance() {
        for (String systemOwned : new String[]{
                "\"source\": {\"type\": \"USER_INPUT\"},",
                "\"originId\": \"11111111-1111-1111-1111-111111111111\",",
                "\"capturedAt\": \"2026-09-03T00:00:00Z\","}) {
            String intruded = draft().replace("\"title\":", systemOwned + " \"title\":");

            assertThatThrownBy(() -> validator.validateAll(intruded, sourceInput))
                    .as(systemOwned)
                    .isInstanceOf(EvidenceExtractionException.class);
        }
    }

    @Test
    @DisplayName("rootCause 필드를 생략하면 거부한다 — null 명시와 생략은 다르다")
    void rejectsOmittedNullableField() {
        String omitted = draft().replace("\"rootCause\": null,", "");

        assertThatThrownBy(() -> validator.validateAll(omitted, sourceInput))
                .isInstanceOf(EvidenceExtractionException.class)
                .hasMessageContaining("rootCause");
    }

    @Test
    @DisplayName("rootCause를 null로 명시하면 통과한다 — '확인했고 근거가 없었다'는 유효한 상태다")
    void acceptsExplicitNull() {
        assertThat(draft()).contains("\"rootCause\": null");
        assertThatCode(() -> validator.validateAll(draft(), sourceInput)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("스키마에 없는 필드를 덧붙이면 거부한다")
    void rejectsUnknownField() {
        String extra = draft().replace("\"title\":", "\"confidence\": 0.9, \"title\":");

        assertThatThrownBy(() -> validator.validateAll(extra, sourceInput))
                .isInstanceOf(EvidenceExtractionException.class);
    }

    @Test
    @DisplayName("excerpt가 원문에 없으면 거부한다 — 스키마는 이걸 잡지 못한다")
    void rejectsFabricatedExcerpt() {
        // 길이는 충분하지만 원문에 없는 문장. "짧아서" 거부되는 것과 구별하기 위해서다.
        String fabricated = draft().replace(
                "다중 SSE 연결에서 최대 14초 지연이 발생했고",
                "SSE 성능 문제를 분석하여 병목 구간을 제거하였습니다");

        assertThatThrownBy(() -> validator.validateAll(fabricated, sourceInput))
                .isInstanceOf(EvidenceExtractionException.class)
                .hasMessageContaining("verbatim");
    }

    @Test
    @DisplayName("원문에 있는 수치를 담은 metrics는 통과한다")
    void acceptsMetricsBackedBySource() {
        String withMetrics = draft().replace("\"metrics\": [],",
                """
                "metrics": [{"name": "JavaMonitorEnter", "before": "341", "after": "0", "unit": null}],
                """);

        assertThatCode(() -> validator.validateAll(withMetrics, sourceInput)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("원문에 없는 수치를 만들어내면 거부한다 — 스키마는 이걸 잡지 못한다")
    void rejectsFabricatedMetrics() {
        String mutated = draft().replace("\"metrics\": [],",
                """
                "metrics": [{"name": "JavaMonitorEnter", "before": "1400", "after": "0", "unit": null}],
                """);

        assertThatThrownBy(() -> validator.validateAll(mutated, sourceInput))
                .isInstanceOf(EvidenceExtractionException.class)
                .hasMessageContaining("1400");
    }

    @Test
    @DisplayName("excerpt가 너무 짧으면 거부한다 — 한 글자는 어떤 원문에도 들어 있다")
    void rejectsTooShortExcerpt() {
        String tiny = draft().replace("다중 SSE 연결에서 최대 14초 지연이 발생했고", "다중");

        assertThatThrownBy(() -> validator.validateAll(tiny, sourceInput))
                .isInstanceOf(EvidenceExtractionException.class);
    }

    @Test
    @DisplayName("evidences 래퍼가 없으면 거부한다 — Structured Output 계약과 같은 모양이어야 한다")
    void rejectsResponseWithoutWrapper() {
        // Evidence 객체를 그대로 던지는 경우 (래퍼 없음)
        assertThatThrownBy(() -> validator.validateAll(singleDraft(), sourceInput))
                .isInstanceOf(EvidenceExtractionException.class)
                .hasMessageContaining("llm.schema.json");

        // 배열을 그대로 던지는 경우 — Structured Output 은 object 루트를 요구한다
        assertThatThrownBy(() -> validator.validateAll("[" + singleDraft() + "]", sourceInput))
                .isInstanceOf(EvidenceExtractionException.class)
                .hasMessageContaining("llm.schema.json");
    }

    @Test
    @DisplayName("원문 하나에서 Evidence 여러 개를 받는다")
    void acceptsMultipleDrafts() {
        assertThat(validator.validateAll(response(singleDraft(), singleDraft()), sourceInput)).hasSize(2);
    }

    @Test
    @DisplayName("하나라도 계약을 어기면 전체를 거부한다 — 부분 저장하지 않는다")
    void rejectsWholeBatchWhenOneItemFails() {
        String bad = singleDraft().replace("\"metrics\": [],",
                "\"metrics\": [{\"name\": \"x\", \"before\": \"9999\", \"after\": \"0\", \"unit\": null}],");
        assertThatThrownBy(() -> validator.validateAll(response(singleDraft(), bad), sourceInput))
                .isInstanceOf(EvidenceExtractionException.class)
                .hasMessageContaining("evidences[1]");
    }

    @Test
    @DisplayName("추출할 경험이 없으면 빈 목록이다 — 억지로 만들지 않는다")
    void acceptsEmptyExtraction() {
        assertThat(validator.validateAll(response(), sourceInput)).isEmpty();
    }

    @Test
    @DisplayName("skills와 category가 비어 있어도 통과한다 — 지어내는 것보다 낫다")
    void acceptsEmptySkillsAndCategory() {
        assertThat(draft()).contains("\"skills\": []").contains("\"category\": []");
        assertThatCode(() -> validator.validateAll(draft(), sourceInput)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("조립 결과가 시스템 계약을 어기면 거부한다 — 우리 코드를 검사하는 관문")
    void rejectsBrokenAssembly() {
        // source 블록이 통째로 빠진 조립 결과. 조립 로직 버그가 이렇게 드러난다.
        String assembledWithoutSource = singleDraft().replace(
                "\"sourceExcerpt\": \"다중 SSE 연결에서 최대 14초 지연이 발생했고\"",
                "\"unused\": null");

        assertThatThrownBy(() -> validator.validateAssembled(parse(assembledWithoutSource)))
                .isInstanceOf(EvidenceExtractionException.class)
                .hasMessageContaining("career-evidence.schema.json");
    }

    @Test
    @DisplayName("조립된 capturedAt 형식이 틀리면 거부한다 — 직렬화 버그를 잡는다")
    void rejectsMalformedAssembledTimestamp() {
        String assembled = singleDraft().replace(
                "\"sourceExcerpt\": \"다중 SSE 연결에서 최대 14초 지연이 발생했고\"",
                """
                "source": {
                  "type": "USER_INPUT",
                  "originId": "11111111-1111-1111-1111-111111111111",
                  "excerpt": "다중 SSE 연결에서 최대 14초 지연이 발생했고",
                  "url": null,
                  "capturedAt": "어제"
                }
                """);

        assertThatThrownBy(() -> validator.validateAssembled(parse(assembled)))
                .isInstanceOf(EvidenceExtractionException.class);
    }

    private com.fasterxml.jackson.databind.JsonNode parse(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 추출기는 evidences 로 감싼 object 를 반환한다. 단건 테스트도 같은 모양으로 감싼다. */
    private String draft() {
        return response(singleDraft());
    }

    private String response(String... evidences) {
        return "{\"evidences\": [" + String.join(",", evidences) + "]}";
    }

    private String singleDraft() {
        return """
                {
                  "title": "SSE 다중 연결 지연 개선",
                  "category": [],
                  "context": { "project": "ESS", "role": null, "period": null, "teamSize": null },
                  "problem": "다중 SSE 연결 환경에서 갱신이 최대 14초 지연",
                  "analysis": "JFR로 락 경합 분석",
                  "rootCause": null,
                  "action": "전송 구조 직렬화",
                  "result": "정체 제거",
                  "metrics": [],
                  "skills": [],
                  "usableFor": [],
                  "sourceExcerpt": "다중 SSE 연결에서 최대 14초 지연이 발생했고"
                }
                """;
    }
}

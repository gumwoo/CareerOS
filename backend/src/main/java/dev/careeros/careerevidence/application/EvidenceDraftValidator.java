package dev.careeros.careerevidence.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import dev.careeros.careerevidence.domain.SourceInput;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 추출 결과가 DB 로 들어가기까지 통과해야 하는 관문.
 *
 * <pre>
 * LLM 출력
 *   -> career-evidence.llm.schema.json   모델이 말할 수 있는 것의 계약
 *   -> 원문 대조 (excerpt / 수치)
 *   -> backend 가 SourceInput 에서 출처를 주입
 *   -> career-evidence.schema.json       시스템 내부 Evidence 의 완성된 모양
 *   -> 도메인
 * </pre>
 *
 * <p>두 스키마를 나눈 이유: 출처(type/originId/url/capturedAt)는 시스템이 이미 아는 값이다.
 * 모델에게 물어봐도 저장할 때는 버리고, 버릴 값의 형식이 틀렸다는 이유로 멀쩡한 추출 전체가
 * 거부될 수 있다. 물어보지 않으면 만들어낼 수도 없다.
 * (Fit 쪽에서 fit-analysis.llm.schema.json 을 나눈 것과 같은 원칙)
 *
 * <p>조립 결과를 다시 검증하는 이유: 그러지 않으면 career-evidence.schema.json 이
 * 런타임에서 아무도 쓰지 않는 계약이 된다. 조립 로직의 필드 누락·잘못된 매핑도 여기서 잡힌다.
 *
 * <p>2번이 별도로 필요한 이유: JSON Schema는 excerpt가 비어 있지 않은 문자열인지까지만
 * 확인할 수 있고, 그것이 원문 그대로인지는 확인하지 못한다. 이 검증이 없으면
 * 추적 체인({@code GeneratedClaim → CareerEvidence → source.originId → 원문})이
 * 형식적으로만 성립하고 실제로는 끊긴다.
 */
@Component
public class EvidenceDraftValidator {

    private static final String LLM_SCHEMA_PATH = "schemas/career-evidence.llm.schema.json";
    private static final String PERSISTED_SCHEMA_PATH = "schemas/career-evidence.schema.json";

    /**
     * Spring이 관리하는 ObjectMapper를 주입받지 않는다.
     *
     * <p>Spring Boot 4는 Jackson 3({@code tools.jackson})을 자동 구성하는데,
     * json-schema-validator는 Jackson 2({@code com.fasterxml.jackson})의 JsonNode를 요구한다.
     * 두 계열을 섞지 않도록 검증 경로에서만 쓰는 Jackson 2 인스턴스를 따로 둔다.
     * HTTP 직렬화는 그대로 Spring의 Jackson 3가 담당한다.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 모델이 말할 수 있는 것. */
    private final JsonSchema llmSchema;

    /** 시스템 내부 Evidence 의 완성된 모양. */
    private final JsonSchema persistedSchema;

    public EvidenceDraftValidator() {
        this.llmSchema = loadSchema(LLM_SCHEMA_PATH);
        this.persistedSchema = loadSchema(PERSISTED_SCHEMA_PATH);
    }

    private JsonSchema loadSchema(String path) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

        // JSON Schema 2020-12에서 format은 기본적으로 annotation이지 assertion이 아니다.
        // 켜두지 않으면 capturedAt: "어제" 가 스키마를 통과하고 Instant.parse 에서
        // DateTimeParseException -> 500 이 된다. 계약 위반은 422여야 한다.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .formatAssertionsEnabled(true)
                .build();

        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return factory.getSchema(in, config);
        } catch (IOException e) {
            // 스키마가 없으면 검증 없이 저장되는 상황이 되므로 기동 자체를 실패시킨다.
            throw new IllegalStateException("Cannot load " + path
                    + ". build.gradle 의 processResources 가 저장소 루트 schemas/ 를 복사하는지 확인할 것.", e);
        }
    }

    /**
     * 추출 결과 배열 전체를 검증한다.
     *
     * <p><b>하나라도 통과하지 못하면 전체를 거부한다.</b> 일부만 저장하면
     * "모델이 무엇을 냈는가"가 흐려지고, 사용자는 걸러진 항목이 있었다는 사실조차 모른다.
     *
     * @return 검증을 통과한 초안 목록. 추출할 경험이 없었으면 빈 목록.
     * @throws EvidenceExtractionException 하나라도 통과하지 못한 경우. 아무것도 저장하지 않는다.
     */
    public List<JsonNode> validateAll(String draftsJson, SourceInput sourceInput) {
        JsonNode drafts = parse(draftsJson);
        if (!drafts.isArray()) {
            throw new EvidenceExtractionException(
                    "Extractor must return a JSON array of drafts, got: " + drafts.getNodeType());
        }

        List<JsonNode> validated = new ArrayList<>();
        int index = 0;
        for (JsonNode draft : drafts) {
            try {
                validated.add(validateOne(draft, sourceInput));
            } catch (EvidenceExtractionException e) {
                throw new EvidenceExtractionException("drafts[" + index + "]: " + e.getMessage());
            }
            index++;
        }
        return validated;
    }

    private JsonNode validateOne(JsonNode draft, SourceInput sourceInput) {
        assertSatisfies(llmSchema, draft, "career-evidence.llm.schema.json");
        verifyExcerptComesFromSource(draft, sourceInput);
        verifyNumbersComeFromSource(draft, sourceInput);
        return draft;
    }

    /**
     * backend 가 출처를 주입해 조립한 결과가 시스템 계약을 만족하는가.
     *
     * <p>모델이 아니라 <b>우리 코드</b>를 검사하는 관문이다.
     * 조립에서 필드를 빠뜨리거나 capturedAt 직렬화가 어긋나면 여기서 걸린다.
     */
    public void validateAssembled(JsonNode assembled) {
        assertSatisfies(persistedSchema, assembled, "career-evidence.schema.json");
    }

    private void assertSatisfies(JsonSchema schema, JsonNode node, String schemaName) {
        Set<ValidationMessage> violations = schema.validate(node);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(ValidationMessage::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new EvidenceExtractionException("Draft violates " + schemaName + ": " + detail);
        }
    }

    private JsonNode parse(String draftsJson) {
        try {
            return objectMapper.readTree(draftsJson);
        } catch (IOException e) {
            throw new EvidenceExtractionException("Draft is not valid JSON: " + e.getMessage());
        }
    }

    /**
     * 주장된 수치가 원문에 실제로 존재하는지 확인한다.
     *
     * <p>metrics 는 검증을 위해 result 문자열에서 일부러 분리해 둔 필드다.
     * 분리해 놓고 대조하지 않으면 분리한 이유가 사라진다.
     *
     * <p>수치가 틀린 Evidence는 DRAFT로 저장된 뒤 사용자가 "그랬던 것 같다"고 확인하면
     * 그대로 사실이 된다. 사람의 기억에 기대는 방어는 방어가 아니다.
     */
    private void verifyNumbersComeFromSource(JsonNode draft, SourceInput sourceInput) {
        for (JsonNode metric : draft.path("metrics")) {
            String name = metric.path("name").asText("");
            for (String field : new String[]{"before", "after"}) {
                String claimed = metric.path(field).asText("");
                if (!sourceInput.supportsNumbersIn(claimed)) {
                    throw new EvidenceExtractionException(
                            "metrics[%s].%s 의 수치가 원문에 없다: %s. "
                                    .formatted(name, field, sourceInput.numbersNotInSource(claimed))
                                    + "원문에 없는 정량 성과는 만들어낼 수 없다.");
                }
            }
        }
    }

    /**
     * 모델이 고른 구절이 실제 원문에서 그대로 나온 것인가.
     *
     * <p>originId / type 을 모델 출력과 대조하던 검사는 없앴다. 모델은 더 이상 그 값을
     * 말하지 않는다. 프롬프트에 엉뚱한 SourceInput 이 들어가는 버그는 모델에게 UUID 를
     * 베껴 쓰게 해서 잡을 게 아니라 프롬프트 조립 코드를 테스트해서 잡아야 한다.
     */
    private void verifyExcerptComesFromSource(JsonNode draft, SourceInput sourceInput) {
        String excerpt = draft.path("sourceExcerpt").asText("");
        if (!sourceInput.contains(excerpt)) {
            throw new EvidenceExtractionException(
                    "sourceExcerpt is not a verbatim fragment of the original text. "
                            + "요약하거나 윤문한 구절은 허용되지 않으며, "
                            + SourceInput.MIN_EXCERPT_LENGTH + "자 미만도 근거로 인정하지 않는다.");
        }
    }
}

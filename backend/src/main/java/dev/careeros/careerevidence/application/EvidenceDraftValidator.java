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
 * 추출 결과가 도메인으로 들어오기 전 두 단계를 통과해야 한다.
 *
 * <ol>
 *   <li>JSON Schema 검증 — 구조와 필수 필드</li>
 *   <li>excerpt 포함 검증 — {@code source.excerpt}가 실제 원문에서 나온 구절인가</li>
 * </ol>
 *
 * <p>2번이 별도로 필요한 이유: JSON Schema는 excerpt가 비어 있지 않은 문자열인지까지만
 * 확인할 수 있고, 그것이 원문 그대로인지는 확인하지 못한다. 이 검증이 없으면
 * 추적 체인({@code GeneratedClaim → CareerEvidence → source.originId → 원문})이
 * 형식적으로만 성립하고 실제로는 끊긴다.
 */
@Component
public class EvidenceDraftValidator {

    private static final String SCHEMA_PATH = "schemas/career-evidence.schema.json";

    /**
     * Spring이 관리하는 ObjectMapper를 주입받지 않는다.
     *
     * <p>Spring Boot 4는 Jackson 3({@code tools.jackson})을 자동 구성하는데,
     * json-schema-validator는 Jackson 2({@code com.fasterxml.jackson})의 JsonNode를 요구한다.
     * 두 계열을 섞지 않도록 검증 경로에서만 쓰는 Jackson 2 인스턴스를 따로 둔다.
     * HTTP 직렬화는 그대로 Spring의 Jackson 3가 담당한다.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonSchema schema;

    public EvidenceDraftValidator() {
        this.schema = loadSchema();
    }

    private JsonSchema loadSchema() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

        // JSON Schema 2020-12에서 format은 기본적으로 annotation이지 assertion이 아니다.
        // 켜두지 않으면 capturedAt: "어제" 가 스키마를 통과하고 Instant.parse 에서
        // DateTimeParseException -> 500 이 된다. 계약 위반은 422여야 한다.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .formatAssertionsEnabled(true)
                .build();

        try (InputStream in = new ClassPathResource(SCHEMA_PATH).getInputStream()) {
            return factory.getSchema(in, config);
        } catch (IOException e) {
            // 스키마가 없으면 검증 없이 저장되는 상황이 되므로 기동 자체를 실패시킨다.
            throw new IllegalStateException("Cannot load " + SCHEMA_PATH
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
        Set<ValidationMessage> violations = schema.validate(draft);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(ValidationMessage::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new EvidenceExtractionException("Draft violates career-evidence.schema.json: " + detail);
        }

        verifyExcerptComesFromSource(draft, sourceInput);
        verifyNumbersComeFromSource(draft, sourceInput);
        return draft;
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

    private void verifyExcerptComesFromSource(JsonNode draft, SourceInput sourceInput) {
        JsonNode source = draft.path("source");

        String originId = source.path("originId").asText("");
        if (!sourceInput.getId().toString().equals(originId)) {
            throw new EvidenceExtractionException(
                    "source.originId does not match the SourceInput being extracted: " + originId);
        }

        String excerpt = source.path("excerpt").asText("");
        if (!sourceInput.contains(excerpt)) {
            throw new EvidenceExtractionException(
                    "source.excerpt is not a verbatim fragment of the original text. "
                            + "요약하거나 윤문한 excerpt는 허용되지 않으며, "
                            + SourceInput.MIN_EXCERPT_LENGTH + "자 미만도 근거로 인정하지 않는다.");
        }

        // type / capturedAt 은 시스템이 아는 값이다. 추출기가 다른 값을 말하면 신뢰할 수 없다.
        String declaredType = source.path("type").asText("");
        if (!sourceInput.getType().name().equals(declaredType)) {
            throw new EvidenceExtractionException(
                    "source.type does not match the actual SourceInput: " + declaredType
                            + " (actual " + sourceInput.getType() + ")");
        }
    }
}

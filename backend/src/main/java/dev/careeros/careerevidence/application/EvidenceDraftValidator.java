package dev.careeros.careerevidence.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import dev.careeros.careerevidence.domain.SourceInput;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
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
        try (InputStream in = new ClassPathResource(SCHEMA_PATH).getInputStream()) {
            return factory.getSchema(in);
        } catch (IOException e) {
            // 스키마가 없으면 검증 없이 저장되는 상황이 되므로 기동 자체를 실패시킨다.
            throw new IllegalStateException("Cannot load " + SCHEMA_PATH
                    + ". build.gradle 의 processResources 가 저장소 루트 schemas/ 를 복사하는지 확인할 것.", e);
        }
    }

    /**
     * @return 스키마와 원문 대조를 모두 통과한 초안
     * @throws EvidenceExtractionException 통과하지 못한 경우. 저장하지 않는다.
     */
    public JsonNode validate(String draftJson, SourceInput sourceInput) {
        JsonNode draft = parse(draftJson);

        Set<ValidationMessage> violations = schema.validate(draft);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(ValidationMessage::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new EvidenceExtractionException("Draft violates career-evidence.schema.json: " + detail);
        }

        verifyExcerptComesFromSource(draft, sourceInput);
        return draft;
    }

    private JsonNode parse(String draftJson) {
        try {
            return objectMapper.readTree(draftJson);
        } catch (IOException e) {
            throw new EvidenceExtractionException("Draft is not valid JSON: " + e.getMessage());
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
                            + "요약하거나 윤문한 excerpt는 허용되지 않는다.");
        }
    }
}

package dev.careeros.careerevidence.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 계약 스키마를 그대로 보내면 요청이 거부된다.
 *
 * <p>Structured Output 은 JSON Schema 부분집합만 받는다. 우리 계약에는 {@code minLength}
 * 8곳과 {@code minimum} 1곳이 있고, 그대로 보내면 첫 호출에서 400 이 난다.
 * Python·TypeScript SDK 는 자동으로 걷어내지만 Java SDK 는 그러지 않는다.
 *
 * <p>동시에 확인하는 것: <b>제거가 요청에만 적용되고 계약 자체는 그대로인가.</b>
 * 원본이 함께 깎이면 20자 하한 같은 규칙이 조용히 사라진다.
 */
class StructuredOutputSchemaTest {

    private static final List<String> UNSUPPORTED = List.of(
            "minLength", "maxLength", "minimum", "maximum", "multipleOf", "minItems", "maxItems");

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode contractSchema() throws Exception {
        try (InputStream in = new ClassPathResource("schemas/career-evidence.llm.schema.json")
                .getInputStream()) {
            return mapper.readTree(in);
        }
    }

    @Test
    @DisplayName("계약 스키마에는 지원되지 않는 제약이 실제로 들어 있다 — 전제 확인")
    void contractSchemaActuallyContainsUnsupportedKeywords() throws Exception {
        assertThat(keywordsIn(contractSchema()))
                .as("이 전제가 깨지면 이 클래스 자체가 필요 없어진다")
                .isNotEmpty()
                .contains("minLength");
    }

    @Test
    @DisplayName("요청용 사본에서는 지원되지 않는 제약이 전부 사라진다")
    void removesEveryUnsupportedKeyword() throws Exception {
        JsonNode forRequest = StructuredOutputSchema.forRequest(contractSchema());

        assertThat(keywordsIn(forRequest)).isEmpty();
    }

    @Test
    @DisplayName("원본 계약은 건드리지 않는다 — 검증에서는 20자 하한이 살아 있어야 한다")
    void doesNotMutateTheContract() throws Exception {
        JsonNode contract = contractSchema();
        String before = contract.toString();

        StructuredOutputSchema.forRequest(contract);

        assertThat(contract.toString()).isEqualTo(before);
        assertThat(contract.at("/$defs/evidence/properties/sourceExcerpt/minLength").asInt())
                .isEqualTo(20);
    }

    @Test
    @DisplayName("지원되는 것은 남긴다 — $defs / $ref / additionalProperties / required / enum")
    void keepsSupportedConstructs() throws Exception {
        JsonNode forRequest = StructuredOutputSchema.forRequest(contractSchema());

        assertThat(forRequest.at("/properties/evidences/items/$ref").asText())
                .isEqualTo("#/$defs/evidence");
        assertThat(forRequest.at("/$defs/evidence/additionalProperties").asBoolean()).isFalse();
        assertThat(forRequest.at("/additionalProperties").asBoolean()).isFalse();
        assertThat(forRequest.at("/required/0").asText()).isEqualTo("evidences");
        assertThat(forRequest.at("/$defs/evidence/required").size()).isEqualTo(12);
        // 출처는 여전히 모델이 말할 수 없다
        assertThat(forRequest.at("/$defs/evidence/properties/source").isMissingNode()).isTrue();
        assertThat(forRequest.at("/$defs/evidence/properties/sourceExcerpt/type").asText())
                .isEqualTo("string");
    }

    private List<String> keywordsIn(JsonNode node) {
        List<String> found = new ArrayList<>();
        collect(node, found);
        return found;
    }

    private void collect(JsonNode node, List<String> found) {
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(name -> {
                if (UNSUPPORTED.contains(name)) {
                    found.add(name);
                }
            });
        }
        node.forEach(child -> collect(child, found));
    }
}

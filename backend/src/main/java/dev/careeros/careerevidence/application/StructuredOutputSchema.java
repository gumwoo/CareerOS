package dev.careeros.careerevidence.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Set;

/**
 * 계약 스키마를 Structured Output 이 받아들이는 형태로 다듬는다.
 *
 * <p>Structured Output 은 JSON Schema 전체가 아니라 부분집합만 받는다.
 * 길이·범위 제약({@code minLength}, {@code maximum} 등)은 지원되지 않고,
 * 그대로 보내면 요청이 거부된다. Python·TypeScript SDK 는 이것을 자동으로
 * 걷어내지만 Java SDK 는 그러지 않으므로 여기서 한다.
 *
 * <p><b>제거는 요청에만 적용된다.</b> {@link EvidenceDraftValidator} 는 원본 스키마를
 * 그대로 쓰므로 {@code sourceExcerpt} 20자 하한 같은 규칙은 응답을 받은 뒤 그대로 강제된다.
 * 모델에게 요구하지 못할 뿐이지 느슨해지는 것이 아니다.
 *
 * <p>지원되는 것: 기본 타입, {@code enum}, {@code const}, {@code anyOf}, {@code allOf},
 * {@code $ref}/{@code $defs}, 문자열 {@code format}, {@code additionalProperties: false}.
 */
final class StructuredOutputSchema {

    /**
     * Structured Output 이 받지 않는 키워드.
     *
     * <p>이 목록이 낡으면 조용히 400 이 난다. 요청이 거부되면 시끄럽게 실패하므로
     * 무증상은 아니고, 그래서 계약 검사까지 만들지는 않았다.
     */
    private static final Set<String> UNSUPPORTED_KEYWORDS = Set.of(
            "minLength", "maxLength",
            "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf",
            "minItems", "maxItems", "uniqueItems");

    private StructuredOutputSchema() {
    }

    /**
     * @param contractSchema 저장소의 계약 스키마. 원본은 건드리지 않는다.
     * @return 요청에 실을 수 있는 사본
     */
    static ObjectNode forRequest(JsonNode contractSchema) {
        ObjectNode copy = contractSchema.deepCopy();
        strip(copy);
        return copy;
    }

    private static void strip(JsonNode node) {
        if (node instanceof ObjectNode object) {
            List<String> toRemove = new java.util.ArrayList<>();
            object.fieldNames().forEachRemaining(name -> {
                if (UNSUPPORTED_KEYWORDS.contains(name)) {
                    toRemove.add(name);
                }
            });
            toRemove.forEach(object::remove);
            object.forEach(StructuredOutputSchema::strip);
        } else if (node != null && node.isArray()) {
            node.forEach(StructuredOutputSchema::strip);
        }
    }
}

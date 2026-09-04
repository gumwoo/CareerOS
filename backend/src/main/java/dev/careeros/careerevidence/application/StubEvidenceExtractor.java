package dev.careeros.careerevidence.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.careeros.careerevidence.domain.SourceInput;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


/**
 * LLM 연결 전까지 파이프라인을 돌리기 위한 스텁.
 *
 * <p><b>이것은 실제 추출기가 아니다.</b> 원문을 읽고 구조를 판단하지 않는다.
 * 파생할 수 없는 필드는 {@code [STUB]} 접두사를 붙여 사람이 즉시 알아볼 수 있게 한다.
 * 결과가 DRAFT로만 저장되므로 사용자가 확인하지 않는 한 사실이 되지 않는다. (ADR-0003)
 *
 * <p>다만 {@code source.excerpt}만은 원문에서 그대로 잘라낸다.
 * 그래야 {@link EvidenceDraftValidator}의 원문 대조가 형식이 아니라 실제로 동작하는지 확인된다.
 *
 * <p><b>{@code stub} 프로파일에서만 등록된다.</b> 이전에는 {@code @Profile("!llm")}이라
 * 프로파일을 지정하지 않으면 스텁이 기본값이었고, 운영에서 프로파일을 빠뜨리면
 * {@code [STUB] Unknown project}가 조용히 CONFIRMED 사실이 될 수 있었다.
 * 지금은 기본 프로파일에 {@code EvidenceExtractor} 빈이 없어 <b>기동 자체가 실패한다.</b>
 * 잘못된 Evidence를 마지막 문에서 거르는 것보다 애초에 만들어지지 않게 하는 편이 낫다.
 */
@Component
@Profile("stub")
public class StubEvidenceExtractor implements EvidenceExtractor {

    private static final int MAX_EXCERPT_LENGTH = 300;

    /** 검증 경로와 같은 Jackson 2 계열을 쓴다. 이유는 EvidenceDraftValidator 참조. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String extractDraftsJson(SourceInput sourceInput) {
        String excerpt = verbatimExcerpt(sourceInput.getRawText());

        ObjectNode draft = objectMapper.createObjectNode();
        draft.put("title", "[STUB] " + firstLine(sourceInput.getRawText()));

        // 스텁은 원문을 분석하지 않는다. 빈 배열이 허용되므로 분류를 지어내지 않는다.
        draft.putArray("category");

        ObjectNode context = draft.putObject("context");
        context.put("project", "[STUB] not extracted");
        context.putNull("role");
        context.putNull("period");
        context.putNull("teamSize");

        draft.put("problem", "[STUB] " + excerpt);
        // 스텁은 원문을 분석하지 않는다. 추측으로 채우는 대신 null을 명시한다.
        draft.putNull("analysis");
        draft.putNull("rootCause");
        draft.put("action", "[STUB] not extracted");
        draft.put("result", "[STUB] not extracted");
        draft.putArray("metrics");

        draft.putArray("skills");

        draft.putArray("usableFor");

        // 출처는 시스템이 소유한다. 모델이 말할 수 있는 것은 "어느 구간이 근거인가"뿐이다.
        draft.put("sourceExcerpt", excerpt);

        // 스텁은 원문을 쪼개지 못하므로 항상 1건이다. 실제 추출기는 여러 건을 낼 수 있다.
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode evidences = response.putArray("evidences");
        evidences.add(draft);

        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new EvidenceExtractionException("Cannot serialize stub draft: " + e.getMessage());
        }
    }

    /** 원문에서 잘라낸 실제 부분 문자열. 요약하거나 다시 쓰지 않는다. */
    private String verbatimExcerpt(String rawText) {
        String trimmed = rawText.trim();
        return trimmed.length() <= MAX_EXCERPT_LENGTH
                ? trimmed
                : trimmed.substring(0, MAX_EXCERPT_LENGTH);
    }

    private String firstLine(String rawText) {
        String first = rawText.trim().lines().findFirst().orElse("").trim();
        return first.length() <= 80 ? first : first.substring(0, 80);
    }
}

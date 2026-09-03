package dev.careeros.careerevidence.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.careeros.careerevidence.domain.SourceInput;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * LLM 연결 전까지 파이프라인을 돌리기 위한 스텁.
 *
 * <p><b>이것은 실제 추출기가 아니다.</b> 원문을 읽고 구조를 판단하지 않는다.
 * 파생할 수 없는 필드는 {@code [STUB]} 접두사를 붙여 사람이 즉시 알아볼 수 있게 한다.
 * 결과가 DRAFT로만 저장되므로 사용자가 확인하지 않는 한 사실이 되지 않는다. (ADR-0003)
 *
 * <p>다만 {@code source.excerpt}만은 원문에서 그대로 잘라낸다.
 * 그래야 {@link EvidenceDraftValidator}의 원문 대조가 형식이 아니라 실제로 동작하는지 확인된다.
 */
@Component
@Profile("!llm")
public class StubEvidenceExtractor implements EvidenceExtractor {

    private static final int MAX_EXCERPT_LENGTH = 300;

    /** 검증 경로와 같은 Jackson 2 계열을 쓴다. 이유는 EvidenceDraftValidator 참조. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String extractDraftJson(SourceInput sourceInput) {
        String excerpt = verbatimExcerpt(sourceInput.getRawText());

        ObjectNode draft = objectMapper.createObjectNode();
        draft.put("title", "[STUB] " + firstLine(sourceInput.getRawText()));

        ArrayNode categories = draft.putArray("category");
        categories.add("[STUB] Uncategorized");

        ObjectNode context = draft.putObject("context");
        context.put("project", "[STUB] Unknown project");
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

        ArrayNode skills = draft.putArray("skills");
        skills.add("[STUB] Unknown");

        draft.putArray("usableFor");

        ObjectNode source = draft.putObject("source");
        source.put("type", sourceInput.getType().name());
        source.put("originId", sourceInput.getId().toString());
        source.put("excerpt", excerpt);
        source.put("url", sourceInput.getUrl());
        source.put("capturedAt", DateTimeFormatter.ISO_INSTANT.format(sourceInput.getCapturedAt()));

        try {
            return objectMapper.writeValueAsString(draft);
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

package dev.careeros.careerevidence.application;

import com.fasterxml.jackson.databind.JsonNode;
import dev.careeros.careerevidence.domain.CareerEvidence;
import dev.careeros.careerevidence.domain.EvidenceCode;
import dev.careeros.careerevidence.domain.EvidenceStatus;
import dev.careeros.careerevidence.domain.SourceInput;
import dev.careeros.careerevidence.domain.SourceType;
import dev.careeros.careerevidence.infrastructure.CareerEvidenceRepository;
import dev.careeros.careerevidence.infrastructure.SourceInputRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Career Evidence의 생명주기.
 *
 * <pre>
 * 원문 입력 -> 추출 -> 스키마 검증 -> 원문 대조 -> DRAFT -> 사용자 확인 -> CONFIRMED
 * </pre>
 *
 * 근거: ADR-0003. LLM 추출 결과를 곧바로 사실로 인정하지 않는다.
 */
@Service
@Transactional
public class CareerEvidenceService {

    private final SourceInputRepository sourceInputRepository;
    private final CareerEvidenceRepository evidenceRepository;
    private final EvidenceExtractor extractor;
    private final EvidenceDraftValidator validator;

    public CareerEvidenceService(SourceInputRepository sourceInputRepository,
                                 CareerEvidenceRepository evidenceRepository,
                                 EvidenceExtractor extractor,
                                 EvidenceDraftValidator validator) {
        this.sourceInputRepository = sourceInputRepository;
        this.evidenceRepository = evidenceRepository;
        this.extractor = extractor;
        this.validator = validator;
    }

    public SourceInput registerSourceInput(SourceType type, String rawText, String url) {
        return sourceInputRepository.save(SourceInput.create(type, rawText, url));
    }

    /**
     * 원문에서 Evidence 초안을 만든다. 검증을 통과하지 못하면 아무것도 저장하지 않는다.
     */
    public CareerEvidence extractDraft(UUID sourceInputId) {
        SourceInput sourceInput = sourceInputRepository.findById(sourceInputId)
                .orElseThrow(() -> new NoSuchElementException("SourceInput not found: " + sourceInputId));

        String draftJson = extractor.extractDraftJson(sourceInput);
        JsonNode draft = validator.validate(draftJson, sourceInput);

        CareerEvidence evidence = toEvidence(draft, sourceInput);
        return evidenceRepository.save(evidence);
    }

    /**
     * 사용자가 원문과 추출 결과를 대조한 뒤 확인했다.
     */
    public CareerEvidence confirm(UUID evidenceId) {
        CareerEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new NoSuchElementException("CareerEvidence not found: " + evidenceId));
        evidence.confirm();
        return evidence;
    }

    @Transactional(readOnly = true)
    public List<CareerEvidence> findByStatus(EvidenceStatus status) {
        return evidenceRepository.findAllByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public CareerEvidence findById(UUID evidenceId) {
        return evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new NoSuchElementException("CareerEvidence not found: " + evidenceId));
    }

    @Transactional(readOnly = true)
    public SourceInput findSourceInput(UUID sourceInputId) {
        return sourceInputRepository.findById(sourceInputId)
                .orElseThrow(() -> new NoSuchElementException("SourceInput not found: " + sourceInputId));
    }

    private CareerEvidence toEvidence(JsonNode draft, SourceInput sourceInput) {
        JsonNode context = draft.path("context");
        JsonNode source = draft.path("source");

        List<CareerEvidence.MetricDraft> metrics = new ArrayList<>();
        draft.path("metrics").forEach(m -> metrics.add(new CareerEvidence.MetricDraft(
                m.path("name").asText(),
                m.path("before").asText(),
                m.path("after").asText(),
                textOrNull(m, "unit"))));

        return CareerEvidence.builder(EvidenceCode.of(evidenceRepository.nextCodeSequence()))
                .title(draft.path("title").asText())
                .categories(toSet(draft, "category"))
                .contextProject(context.path("project").asText())
                .contextRole(textOrNull(context, "role"))
                .contextPeriod(textOrNull(context, "period"))
                .contextTeamSize(context.path("teamSize").isIntegralNumber()
                        ? context.path("teamSize").asInt() : null)
                .problem(draft.path("problem").asText())
                .analysis(textOrNull(draft, "analysis"))
                .rootCause(textOrNull(draft, "rootCause"))
                .action(draft.path("action").asText())
                .result(draft.path("result").asText())
                .metrics(metrics)
                .skills(toSet(draft, "skills"))
                .usableFor(toSet(draft, "usableFor"))
                .source(SourceType.valueOf(source.path("type").asText()),
                        sourceInput.getId(),
                        source.path("excerpt").asText(),
                        textOrNull(source, "url"),
                        Instant.parse(source.path("capturedAt").asText()))
                .build();
    }

    private static Set<String> toSet(JsonNode node, String field) {
        Set<String> values = new LinkedHashSet<>();
        node.path(field).forEach(v -> values.add(v.asText()));
        return values;
    }

    /** null과 "없음"을 구별한다. JSON의 null은 Java의 null로 유지한다. */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNull() || value.isMissingNode() ? null : value.asText();
    }
}

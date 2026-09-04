package dev.careeros.careerevidence.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.careeros.careerevidence.domain.CareerEvidence;
import dev.careeros.careerevidence.domain.EvidenceCode;
import dev.careeros.careerevidence.domain.EvidenceStatus;
import dev.careeros.careerevidence.domain.SourceInput;
import dev.careeros.careerevidence.domain.SourceType;
import dev.careeros.careerevidence.infrastructure.CareerEvidenceRepository;
import dev.careeros.careerevidence.infrastructure.SourceInputRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
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
     * 원문 하나에서 Evidence 초안을 여러 개 만든다.
     *
     * <p>이력서나 프로젝트 설명 하나에 독립적인 경험이 여러 개 들어 있는 것이 정상이다.
     * 하나로 뭉치면 Fit Analysis 에서 공고별로 재사용할 수 없다.
     *
     * <p>하나라도 검증을 통과하지 못하면 <b>아무것도 저장하지 않는다.</b>
     * 일부만 저장하면 걸러진 항목이 있었다는 사실이 사용자에게 보이지 않는다.
     *
     * @return 저장된 DRAFT 목록. 추출할 경험이 없었으면 빈 목록.
     */
    public List<CareerEvidence> extractDrafts(UUID sourceInputId) {
        SourceInput sourceInput = sourceInputRepository.findById(sourceInputId)
                .orElseThrow(() -> new NoSuchElementException("SourceInput not found: " + sourceInputId));

        String draftsJson = extractor.extractDraftsJson(sourceInput);
        List<JsonNode> drafts = validator.validateAll(draftsJson, sourceInput);

        List<CareerEvidence> evidences = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            try {
                // 모델은 출처를 말하지 않는다. 여기서 시스템이 아는 값을 주입해 조립하고,
                // 조립 결과가 시스템 계약(career-evidence.schema.json)을 만족하는지 다시 본다.
                JsonNode assembled = assembleWithSource(drafts.get(i), sourceInput);
                validator.validateAssembled(assembled);
                evidences.add(toEvidence(assembled, sourceInput));
            } catch (EvidenceExtractionException e) {
                throw new EvidenceExtractionException("drafts[" + i + "]: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                // 도메인 불변식 위반은 "클라이언트 요청이 잘못됨"(400)이 아니라
                // "추출 결과가 계약을 어김"(422)이다. 둘을 섞으면 계약 위반율을 집계할 수 없다.
                throw new EvidenceExtractionException(
                        "drafts[" + i + "] violates a domain invariant: " + e.getMessage());
            }
        }
        return evidenceRepository.saveAll(evidences);
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

    /**
     * 모델 출력에 출처를 붙여 완성된 Evidence 모양으로 만든다.
     *
     * <pre>
     * source.type       &lt;- SourceInput
     * source.originId   &lt;- SourceInput
     * source.url        &lt;- SourceInput
     * source.capturedAt &lt;- SourceInput
     * source.excerpt    &lt;- 모델이 고른 sourceExcerpt
     * </pre>
     */
    private JsonNode assembleWithSource(JsonNode llmDraft, SourceInput sourceInput) {
        ObjectNode assembled = llmDraft.deepCopy();
        String excerpt = assembled.path("sourceExcerpt").asText();
        assembled.remove("sourceExcerpt");

        ObjectNode source = assembled.putObject("source");
        source.put("type", sourceInput.getType().name());
        source.put("originId", sourceInput.getId().toString());
        source.put("excerpt", excerpt);
        if (sourceInput.getUrl() == null) {
            source.putNull("url");
        } else {
            source.put("url", sourceInput.getUrl());
        }
        source.put("capturedAt", DateTimeFormatter.ISO_INSTANT.format(sourceInput.getCapturedAt()));
        return assembled;
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
                // type / originId / url / capturedAt 은 추출기 출력이 아니라 SourceInput 에서 온다.
                // 추출기에게 물어볼 이유가 없는 값이고, 물어보면 실제 입력 경로와 어긋날 수 있다.
                // (excerpt 만 추출기가 고른 값이고, 그래서 원문 대조 대상이다)
                .source(sourceInput.getType(),
                        sourceInput.getId(),
                        source.path("excerpt").asText(),
                        sourceInput.getUrl(),
                        sourceInput.getCapturedAt())
                .buildVerifiedAgainst(sourceInput);
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

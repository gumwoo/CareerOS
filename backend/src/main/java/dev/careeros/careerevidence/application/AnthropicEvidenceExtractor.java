package dev.careeros.careerevidence.application;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.careeros.careerevidence.domain.SourceInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 실제 추출기.
 *
 * <p>모델에게 넘기는 응답 스키마는 <b>{@code schemas/career-evidence.llm.schema.json} 파일
 * 그 자체</b>다. POJO에서 스키마를 파생시키지 않는다. 파생시키면 저장소의 계약과 모델이
 * 실제로 받는 계약이 갈라지고, 그때부터 {@code schemas/}는 아무것도 보장하지 않는 문서가 된다.
 *
 * <p>이 클래스는 응답을 검증하지 않는다. 검증은 {@link EvidenceDraftValidator}가 한다.
 * 추출기가 스스로 검증하면 "모델이 계약을 지켰는가"를 모델을 부른 쪽이 판정하는 셈이 된다.
 *
 * <p>{@code stub} 프로파일이 아닐 때 등록된다. 즉 <b>이것이 기본값</b>이고,
 * API 키가 없으면 기동이 실패한다. 키 없이 돌리려면 {@code stub} 프로파일을 명시해야 한다.
 */
@Component
@Profile("!stub")
public class AnthropicEvidenceExtractor implements EvidenceExtractor {

    private static final Logger log = LoggerFactory.getLogger(AnthropicEvidenceExtractor.class);
    private static final String SCHEMA_PATH = "schemas/career-evidence.llm.schema.json";

    private final AnthropicClient client;
    private final EvidencePromptBuilder promptBuilder;
    private final EvidenceExtractionProperties properties;
    private final JsonValue responseSchema;

    public AnthropicEvidenceExtractor(AnthropicClient client,
                                      EvidencePromptBuilder promptBuilder,
                                      EvidenceExtractionProperties properties) {
        this.client = client;
        this.promptBuilder = promptBuilder;
        this.properties = properties;
        this.responseSchema = loadResponseSchema();
    }

    @Override
    public String extractDraftsJson(SourceInput sourceInput) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(properties.maxTokens())
                // 시스템 프롬프트와 스키마는 요청마다 동일하다. 캐시해야 반복 호출 비용이 내려간다.
                .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                        .text(promptBuilder.systemPrompt())
                        .cacheControl(CacheControlEphemeral.builder().build())
                        .build()))
                .outputConfig(OutputConfig.builder()
                        .format(JsonOutputFormat.builder().schema(responseSchema).build())
                        .effort(properties.effort())
                        .build())
                .thinking(ThinkingConfigAdaptive.builder().build())
                .addUserMessage(promptBuilder.userMessage(sourceInput))
                .build();

        Message response = client.messages().create(params);
        logUsage(response);
        rejectIfNotCompleted(response, sourceInput);

        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow(() -> new EvidenceExtractionException(
                        "Model returned no text block for sourceInput " + sourceInput.getId()));
    }

    /**
     * 응답이 끝까지 나오지 않았으면 거부한다.
     *
     * <p>{@code MAX_TOKENS}로 잘린 JSON은 파싱조차 되지 않아 어차피 실패하지만,
     * 그때는 "JSON 파싱 실패"로만 보여서 원인이 max_tokens 라는 것이 드러나지 않는다.
     * {@code REFUSAL}은 200으로 돌아오므로 확인하지 않으면 조용히 빈 결과가 된다.
     */
    private void rejectIfNotCompleted(Message response, SourceInput sourceInput) {
        StopReason stopReason = response.stopReason().orElse(null);
        if (stopReason == null || stopReason.equals(StopReason.END_TURN)) {
            return;
        }
        throw new EvidenceExtractionException(
                "Model did not complete the extraction (stopReason=" + stopReason
                        + ", sourceInput=" + sourceInput.getId() + ")");
    }

    /** 비용과 캐시 적중을 눈으로 확인할 수 있어야 한다. 캐시가 0이면 프롬프트가 흔들리고 있다는 뜻이다. */
    private void logUsage(Message response) {
        var usage = response.usage();
        log.debug("evidence extraction usage: in={} out={} cacheWrite={} cacheRead={} model={} prompt={}",
                usage.inputTokens(), usage.outputTokens(),
                usage.cacheCreationInputTokens().orElse(0L),
                usage.cacheReadInputTokens().orElse(0L),
                properties.model(), promptBuilder.version());
    }

    /**
     * 저장소의 계약 파일을 그대로 응답 스키마로 넘긴다.
     *
     * <p>{@code processResources}가 저장소 루트 {@code schemas/}를 복사해 온 것이라
     * Validator가 검증에 쓰는 파일과 <b>같은 파일</b>이다. 요청 스키마와 검증 스키마가
     * 다른 출처에서 오면 둘이 갈라져도 아무도 모른다.
     */
    private JsonValue loadResponseSchema() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = new ClassPathResource(SCHEMA_PATH).getInputStream()) {
            // Structured Output 이 받지 않는 제약은 요청에서만 걷어낸다.
            // 검증은 원본 스키마로 그대로 하므로 규칙이 느슨해지지는 않는다.
            JsonNode forRequest = StructuredOutputSchema.forRequest(mapper.readTree(in));
            return JsonValue.from(mapper.convertValue(forRequest, Map.class));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + SCHEMA_PATH, e);
        }
    }
}

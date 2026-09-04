package dev.careeros.careerevidence.application;

import com.anthropic.models.messages.OutputConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 추출 호출 설정.
 *
 * <p>모델을 설정값으로 빼 두는 이유는 <b>측정해서 내리기 위해서</b>다.
 * Sonnet 5 로 시작하지만, AI Eval 데이터셋이 생기면 같은 입력으로 Haiku 4.5 를 돌려
 * 정확도가 유지되는지 보고 결정한다. 감으로 정하지 않는다.
 *
 * <p>프롬프트 버전도 설정값이다. 프롬프트를 바꾸면 결과가 바뀌므로,
 * 어떤 버전으로 뽑은 Evidence 인지 추적할 수 있어야 한다.
 *
 * @param model        모델 id. 예: {@code claude-sonnet-5}
 * @param maxTokens    응답 상한. Evidence 여러 건이 나올 수 있어 넉넉히 둔다.
 * @param effort       사고 깊이. 추출은 창작이 아니라 정밀 작업이라 기본 HIGH.
 * @param promptVersion {@code prompts/evidence-extraction.<version>.md} 의 버전
 */
@ConfigurationProperties(prefix = "careeros.evidence-extraction")
public record EvidenceExtractionProperties(
        String model,
        Long maxTokens,
        OutputConfig.Effort effort,
        String promptVersion) {

    public EvidenceExtractionProperties {
        if (model == null || model.isBlank()) {
            model = "claude-sonnet-5";
        }
        if (maxTokens == null) {
            maxTokens = 16000L;
        }
        if (effort == null) {
            effort = OutputConfig.Effort.HIGH;
        }
        if (promptVersion == null || promptVersion.isBlank()) {
            promptVersion = "v1";
        }
    }
}

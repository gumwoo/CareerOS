package dev.careeros.careerevidence.application;

/**
 * 이 Evidence를 <b>무엇이</b> 만들었는가.
 *
 * <p>{@link dev.careeros.careerevidence.domain.SourceInput}이 "어떤 원문에서 왔는가"라면
 * 이것은 "어떤 추출기가 어떤 프롬프트로 뽑았는가"다. 둘 다 있어야 나중에
 * Evidence 하나를 보고 다음 질문에 답할 수 있다.
 *
 * <pre>
 * 이건 Sonnet 5가 만든 건가, Haiku 실험 때 만든 건가?
 * 프롬프트 v1인가 v2인가?
 * </pre>
 *
 * <p>모델과 프롬프트를 바꾸면 결과가 바뀐다. 어떤 조합으로 뽑은 것인지 기록하지 않으면
 * AI Eval 로 "v2가 v1보다 나은가"를 비교할 때 이미 쌓인 Evidence 를 쓸 수 없다.
 * 로그로만 남기면 DB 를 봐도 알 수 없다.
 *
 * @param model         모델 id. 실제 모델이 아닌 추출기는 자기 이름을 쓴다(예: {@code stub}).
 * @param promptVersion 프롬프트 버전. 프롬프트를 쓰지 않는 추출기는 {@code null}.
 */
public record ExtractionOrigin(String model, String promptVersion) {

    public ExtractionOrigin {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("ExtractionOrigin requires a non-blank model");
        }
    }

    /** 프롬프트를 쓰지 않는 추출기용. */
    public static ExtractionOrigin of(String model) {
        return new ExtractionOrigin(model, null);
    }
}

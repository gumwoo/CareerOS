package dev.careeros.careerevidence.api;

import dev.careeros.careerevidence.domain.CareerEvidence;
import dev.careeros.careerevidence.domain.EvidenceMetric;
import dev.careeros.careerevidence.domain.SourceInput;
import dev.careeros.careerevidence.domain.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CareerEvidenceDtos {

    private CareerEvidenceDtos() {
    }

    record CreateSourceInputRequest(
            @NotNull SourceType type,
            @NotBlank String rawText,
            String url) {
    }

    record SourceInputResponse(UUID id, SourceType type, String rawText, String url, Instant capturedAt) {

        static SourceInputResponse from(SourceInput input) {
            return new SourceInputResponse(input.getId(), input.getType(),
                    input.getRawText(), input.getUrl(), input.getCapturedAt());
        }
    }

    record ExtractRequest(@NotNull UUID sourceInputId) {
    }

    record MetricResponse(String name, String before, String after, String unit) {

        static MetricResponse from(EvidenceMetric metric) {
            return new MetricResponse(metric.getName(), metric.getBeforeValue(),
                    metric.getAfterValue(), metric.getUnit());
        }
    }

    record SourceResponse(SourceType type, UUID originId, String excerpt, String url, Instant capturedAt) {
    }

    record EvidenceResponse(
            UUID id,
            String code,
            String status,
            String title,
            Set<String> category,
            ContextResponse context,
            String problem,
            String analysis,
            String rootCause,
            String action,
            String result,
            List<MetricResponse> metrics,
            Set<String> skills,
            Set<String> usableFor,
            SourceResponse source) {

        static EvidenceResponse from(CareerEvidence e) {
            return new EvidenceResponse(
                    e.getId(),
                    e.getCode(),
                    e.getStatus().name(),
                    e.getTitle(),
                    e.getCategories(),
                    new ContextResponse(e.getContextProject(), e.getContextRole(),
                            e.getContextPeriod(), e.getContextTeamSize()),
                    e.getProblem(),
                    e.getAnalysis(),
                    e.getRootCause(),
                    e.getAction(),
                    e.getResult(),
                    e.getMetrics().stream().map(MetricResponse::from).toList(),
                    e.getSkills(),
                    e.getUsableFor(),
                    new SourceResponse(e.getSourceType(), e.getSourceOriginId(),
                            e.getSourceExcerpt(), e.getSourceUrl(), e.getSourceCapturedAt()));
        }
    }

    record ContextResponse(String project, String role, String period, Integer teamSize) {
    }

    /**
     * 추출 결과는 원문과 <b>함께</b> 보여준다.
     * 사용자가 대조하지 못하면 확인 단계가 형식적인 클릭이 된다.
     *
     * <p>원문 하나에서 Evidence가 여러 개 나올 수 있으므로 목록으로 담는다.
     * 확인(confirm)은 초안별로 따로 한다 — 하나는 맞고 하나는 틀릴 수 있다.
     */
    record DraftReviewResponse(List<EvidenceResponse> evidences, SourceInputResponse source) {
    }
}

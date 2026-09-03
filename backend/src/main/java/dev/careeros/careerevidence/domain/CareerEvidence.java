package dev.careeros.careerevidence.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 사용자 경력의 유일한 사실 출처. (ADR-0003)
 *
 * <p>이 클래스가 강제하는 것:
 * <ul>
 *   <li>source 없이는 생성할 수 없다 — 출처 없는 Evidence는 출처를 알 수 없는 주장이다.</li>
 *   <li>LLM 추출 직후는 DRAFT다. 사용자가 확인해야 CONFIRMED가 된다.</li>
 *   <li>analysis / rootCause는 null일 수 있다. null은 "확인했고 근거가 없었다"는 뜻이며,
 *       추측으로 채우는 것과 다르다.</li>
 * </ul>
 */
@Entity
@Table(name = "career_evidences")
public class CareerEvidence {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceStatus status;

    @Column(nullable = false)
    private String title;

    @Column(name = "context_project", nullable = false)
    private String contextProject;

    @Column(name = "context_role")
    private String contextRole;

    @Column(name = "context_period")
    private String contextPeriod;

    @Column(name = "context_team_size")
    private Integer contextTeamSize;

    @Column(nullable = false)
    private String problem;

    /** 근거가 없으면 null. 필드를 생략하는 것과 다르다. */
    private String analysis;

    /** 원문이 원인을 밝히지 않았으면 null. 그럴듯한 추측을 넣지 않는다. */
    @Column(name = "root_cause")
    private String rootCause;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String result;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_origin_id", nullable = false)
    private UUID sourceOriginId;

    /** 근거가 된 원문 구절 그대로. 요약본이 아니다. */
    @Column(name = "source_excerpt", nullable = false)
    private String sourceExcerpt;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "source_captured_at", nullable = false)
    private Instant sourceCapturedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "career_evidence_categories", joinColumns = @JoinColumn(name = "evidence_id"))
    @Column(name = "value", nullable = false)
    private Set<String> categories = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "career_evidence_skills", joinColumns = @JoinColumn(name = "evidence_id"))
    @Column(name = "value", nullable = false)
    private Set<String> skills = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "career_evidence_usable_for", joinColumns = @JoinColumn(name = "evidence_id"))
    @Column(name = "value", nullable = false)
    private Set<String> usableFor = new LinkedHashSet<>();

    @OneToMany(mappedBy = "evidence", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EvidenceMetric> metrics = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CareerEvidence() {
        // JPA
    }

    private CareerEvidence(Builder builder) {
        requireText(builder.title, "title");
        requireText(builder.contextProject, "context.project");
        requireText(builder.problem, "problem");
        requireText(builder.action, "action");
        requireText(builder.result, "result");
        requireText(builder.sourceExcerpt, "source.excerpt");
        if (builder.sourceType == null || builder.sourceOriginId == null || builder.sourceCapturedAt == null) {
            throw new IllegalArgumentException("CareerEvidence requires a complete source");
        }
        if (builder.categories.isEmpty()) {
            throw new IllegalArgumentException("CareerEvidence requires at least one category");
        }
        if (builder.skills.isEmpty()) {
            throw new IllegalArgumentException("CareerEvidence requires at least one skill");
        }

        this.id = UUID.randomUUID();
        this.code = builder.code.value();
        this.status = EvidenceStatus.DRAFT;
        this.title = builder.title;
        this.contextProject = builder.contextProject;
        this.contextRole = builder.contextRole;
        this.contextPeriod = builder.contextPeriod;
        this.contextTeamSize = builder.contextTeamSize;
        this.problem = builder.problem;
        this.analysis = builder.analysis;
        this.rootCause = builder.rootCause;
        this.action = builder.action;
        this.result = builder.result;
        this.sourceType = builder.sourceType;
        this.sourceOriginId = builder.sourceOriginId;
        this.sourceExcerpt = builder.sourceExcerpt;
        this.sourceUrl = builder.sourceUrl;
        this.sourceCapturedAt = builder.sourceCapturedAt;
        this.categories.addAll(builder.categories);
        this.skills.addAll(builder.skills);
        this.usableFor.addAll(builder.usableFor);
        builder.metrics.forEach(m ->
                this.metrics.add(new EvidenceMetric(this, m.name(), m.before(), m.after(), m.unit())));
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CareerEvidence requires non-blank " + field);
        }
    }

    /**
     * 사용자가 추출 결과를 확인했다. 이 시점부터 Fit Analysis 등이 사실로 사용한다.
     */
    public void confirm() {
        if (status == EvidenceStatus.CONFIRMED) {
            throw new IllegalStateException("Already confirmed: " + code);
        }
        this.status = EvidenceStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public record MetricDraft(String name, String before, String after, String unit) {
    }

    public static Builder builder(EvidenceCode code) {
        return new Builder(code);
    }

    public static final class Builder {
        private final EvidenceCode code;
        private String title;
        private String contextProject;
        private String contextRole;
        private String contextPeriod;
        private Integer contextTeamSize;
        private String problem;
        private String analysis;
        private String rootCause;
        private String action;
        private String result;
        private SourceType sourceType;
        private UUID sourceOriginId;
        private String sourceExcerpt;
        private String sourceUrl;
        private Instant sourceCapturedAt;
        private Set<String> categories = new LinkedHashSet<>();
        private Set<String> skills = new LinkedHashSet<>();
        private Set<String> usableFor = new LinkedHashSet<>();
        private List<MetricDraft> metrics = new ArrayList<>();

        private Builder(EvidenceCode code) {
            if (code == null) {
                throw new IllegalArgumentException("CareerEvidence requires a code");
            }
            this.code = code;
        }

        public Builder title(String v) { this.title = v; return this; }
        public Builder contextProject(String v) { this.contextProject = v; return this; }
        public Builder contextRole(String v) { this.contextRole = v; return this; }
        public Builder contextPeriod(String v) { this.contextPeriod = v; return this; }
        public Builder contextTeamSize(Integer v) { this.contextTeamSize = v; return this; }
        public Builder problem(String v) { this.problem = v; return this; }
        public Builder analysis(String v) { this.analysis = v; return this; }
        public Builder rootCause(String v) { this.rootCause = v; return this; }
        public Builder action(String v) { this.action = v; return this; }
        public Builder result(String v) { this.result = v; return this; }
        public Builder categories(Set<String> v) { this.categories = new LinkedHashSet<>(v); return this; }
        public Builder skills(Set<String> v) { this.skills = new LinkedHashSet<>(v); return this; }
        public Builder usableFor(Set<String> v) { this.usableFor = new LinkedHashSet<>(v); return this; }
        public Builder metrics(List<MetricDraft> v) { this.metrics = new ArrayList<>(v); return this; }

        public Builder source(SourceType type, UUID originId, String excerpt, String url, Instant capturedAt) {
            this.sourceType = type;
            this.sourceOriginId = originId;
            this.sourceExcerpt = excerpt;
            this.sourceUrl = url;
            this.sourceCapturedAt = capturedAt;
            return this;
        }

        /**
         * 원문을 넘겨야만 생성된다.
         *
         * <p>excerpt 원문 대조를 application 계층에만 두면, 리포지토리를 직접 쓰는
         * 다음 기능(배치 임포트, GitHub 연동 등)이 그 검증을 건너뛸 수 있다.
         * 도메인이 원문을 요구하면 우회 경로 자체가 사라진다. (ADR-0003)
         */
        public CareerEvidence buildVerifiedAgainst(SourceInput sourceInput) {
            if (sourceInput == null) {
                throw new IllegalArgumentException("CareerEvidence requires the SourceInput it came from");
            }
            if (!sourceInput.getId().equals(sourceOriginId)) {
                throw new IllegalArgumentException(
                        "source.originId does not match the given SourceInput: " + sourceOriginId);
            }
            if (!sourceInput.contains(sourceExcerpt)) {
                throw new IllegalArgumentException(
                        "source.excerpt is not a verbatim fragment of the original text");
            }
            return new CareerEvidence(this);
        }
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public EvidenceStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getContextProject() { return contextProject; }
    public String getContextRole() { return contextRole; }
    public String getContextPeriod() { return contextPeriod; }
    public Integer getContextTeamSize() { return contextTeamSize; }
    public String getProblem() { return problem; }
    public String getAnalysis() { return analysis; }
    public String getRootCause() { return rootCause; }
    public String getAction() { return action; }
    public String getResult() { return result; }
    public SourceType getSourceType() { return sourceType; }
    public UUID getSourceOriginId() { return sourceOriginId; }
    public String getSourceExcerpt() { return sourceExcerpt; }
    public String getSourceUrl() { return sourceUrl; }
    public Instant getSourceCapturedAt() { return sourceCapturedAt; }
    public Set<String> getCategories() { return Set.copyOf(categories); }
    public Set<String> getSkills() { return Set.copyOf(skills); }
    public Set<String> getUsableFor() { return Set.copyOf(usableFor); }
    public List<EvidenceMetric> getMetrics() { return List.copyOf(metrics); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

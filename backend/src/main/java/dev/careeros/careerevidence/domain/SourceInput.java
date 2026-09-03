package dev.careeros.careerevidence.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 사용자가 실제로 입력한 원문. Career Evidence 추적 체인의 끝이다.
 *
 * <pre>
 * GeneratedClaim.evidenceRef -> CareerEvidence -> source.originId -> SourceInput.rawText
 * </pre>
 *
 * 근거: ADR-0003
 */
@Entity
@Table(name = "source_inputs")
public class SourceInput {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType type;

    @Column(name = "raw_text", nullable = false)
    private String rawText;

    private String url;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SourceInput() {
        // JPA
    }

    private SourceInput(UUID id, SourceType type, String rawText, String url, Instant capturedAt) {
        if (type == null) {
            throw new IllegalArgumentException("SourceInput requires a type");
        }
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("SourceInput requires non-blank rawText");
        }
        this.id = id;
        this.type = type;
        this.rawText = rawText;
        this.url = url;
        this.capturedAt = capturedAt;
        this.createdAt = Instant.now();
    }

    public static SourceInput create(SourceType type, String rawText, String url) {
        return new SourceInput(UUID.randomUUID(), type, rawText, url, Instant.now());
    }

    /**
     * excerpt가 이 원문에서 실제로 나온 구절인지 확인한다.
     *
     * <p>JSON Schema는 excerpt가 비어 있지 않은 문자열인지까지만 검증할 수 있고,
     * "원문 그대로인가"는 검증하지 못한다. 그 구멍을 여기서 막는다.
     *
     * <p>공백 차이만 정규화한다. 단어를 바꾸거나 요약한 excerpt는 통과하지 못한다.
     */
    public boolean contains(String excerpt) {
        if (excerpt == null || excerpt.isBlank()) {
            return false;
        }
        return normalize(rawText).contains(normalize(excerpt));
    }

    private static String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    public UUID getId() {
        return id;
    }

    public SourceType getType() {
        return type;
    }

    public String getRawText() {
        return rawText;
    }

    public String getUrl() {
        return url;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

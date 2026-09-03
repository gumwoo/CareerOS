package dev.careeros.careerevidence.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 정량 성과. result 문자열에서 분리해 둔다.
 *
 * <p>"JavaMonitorEnter 341 → 0"이 result 안에 묻혀 있으면 나중에 값이 변형됐는지
 * (예: "대폭 감소"로 바뀌는 것) 기계적으로 확인할 수 없다.
 */
@Entity
@Table(name = "career_evidence_metrics")
public class EvidenceMetric {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "evidence_id", nullable = false)
    private CareerEvidence evidence;

    @Column(nullable = false)
    private String name;

    @Column(name = "before_value", nullable = false)
    private String beforeValue;

    @Column(name = "after_value", nullable = false)
    private String afterValue;

    private String unit;

    protected EvidenceMetric() {
        // JPA
    }

    EvidenceMetric(CareerEvidence evidence, String name, String beforeValue, String afterValue, String unit) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Metric requires a name");
        }
        if (beforeValue == null || afterValue == null) {
            throw new IllegalArgumentException("Metric requires before and after values: " + name);
        }
        this.id = UUID.randomUUID();
        this.evidence = evidence;
        this.name = name;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.unit = unit;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }

    public String getUnit() {
        return unit;
    }
}

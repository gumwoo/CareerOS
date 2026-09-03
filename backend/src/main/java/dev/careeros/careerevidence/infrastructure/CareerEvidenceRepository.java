package dev.careeros.careerevidence.infrastructure;

import dev.careeros.careerevidence.domain.CareerEvidence;
import dev.careeros.careerevidence.domain.EvidenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareerEvidenceRepository extends JpaRepository<CareerEvidence, UUID> {

    List<CareerEvidence> findAllByStatusOrderByCreatedAtDesc(EvidenceStatus status);

    Optional<CareerEvidence> findByCode(String code);

    /**
     * 사용자 노출 코드(CE-00001)는 시스템이 부여한다. LLM이 만들지 않는다.
     */
    @Query(value = "select nextval('career_evidence_code_seq')", nativeQuery = true)
    long nextCodeSequence();
}

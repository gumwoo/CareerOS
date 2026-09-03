package dev.careeros.careerevidence.api;

import dev.careeros.careerevidence.api.CareerEvidenceDtos.CreateSourceInputRequest;
import dev.careeros.careerevidence.api.CareerEvidenceDtos.DraftReviewResponse;
import dev.careeros.careerevidence.api.CareerEvidenceDtos.EvidenceResponse;
import dev.careeros.careerevidence.api.CareerEvidenceDtos.ExtractRequest;
import dev.careeros.careerevidence.api.CareerEvidenceDtos.SourceInputResponse;
import dev.careeros.careerevidence.application.CareerEvidenceService;
import dev.careeros.careerevidence.domain.CareerEvidence;
import dev.careeros.careerevidence.domain.EvidenceStatus;
import dev.careeros.careerevidence.domain.SourceInput;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CareerEvidenceController {

    private final CareerEvidenceService service;

    public CareerEvidenceController(CareerEvidenceService service) {
        this.service = service;
    }

    /** 1단계. 사용자가 실제로 쓴 원문을 먼저 저장한다. 이것이 추적 체인의 끝이다. */
    @PostMapping("/source-inputs")
    public ResponseEntity<SourceInputResponse> createSourceInput(
            @Valid @RequestBody CreateSourceInputRequest request) {
        SourceInput saved = service.registerSourceInput(request.type(), request.rawText(), request.url());
        return ResponseEntity.status(HttpStatus.CREATED).body(SourceInputResponse.from(saved));
    }

    /**
     * 2단계. 추출 -> 스키마 검증 -> 원문 대조 -> DRAFT 저장.
     * 응답에 원문을 함께 담아 사용자가 대조할 수 있게 한다.
     */
    @PostMapping("/career-evidences/extract")
    public ResponseEntity<DraftReviewResponse> extract(@Valid @RequestBody ExtractRequest request) {
        CareerEvidence draft = service.extractDraft(request.sourceInputId());
        SourceInput source = service.findSourceInput(request.sourceInputId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DraftReviewResponse(EvidenceResponse.from(draft), SourceInputResponse.from(source)));
    }

    /** 3단계. 사용자 확인. 이 시점부터 사실로 사용된다. */
    @PostMapping("/career-evidences/{id}/confirm")
    public EvidenceResponse confirm(@PathVariable UUID id) {
        return EvidenceResponse.from(service.confirm(id));
    }

    /** 4단계. Career Bank 조회. 기본값은 CONFIRMED만 본다. */
    @GetMapping("/career-evidences")
    public List<EvidenceResponse> list(
            @RequestParam(defaultValue = "CONFIRMED") EvidenceStatus status) {
        return service.findByStatus(status).stream().map(EvidenceResponse::from).toList();
    }

    @GetMapping("/career-evidences/{id}")
    public EvidenceResponse get(@PathVariable UUID id) {
        return EvidenceResponse.from(service.findById(id));
    }
}

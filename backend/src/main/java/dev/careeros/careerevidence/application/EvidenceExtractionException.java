package dev.careeros.careerevidence.application;

/** 추출 결과가 계약을 지키지 못했다. 저장하지 않고 실패시킨다. */
public class EvidenceExtractionException extends RuntimeException {

    public EvidenceExtractionException(String message) {
        super(message);
    }
}

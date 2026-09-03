package dev.careeros.careerevidence.domain;

import java.util.regex.Pattern;

/**
 * CE-00001 형태의 사용자 노출 식별자. LLM이 생성하지 않고 시스템이 부여한다.
 */
public record EvidenceCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^CE-\\d{5}$");

    public EvidenceCode {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Evidence code must match CE-00000: " + value);
        }
    }

    public static EvidenceCode of(long sequence) {
        return new EvidenceCode("CE-%05d".formatted(sequence));
    }

    @Override
    public String toString() {
        return value;
    }
}

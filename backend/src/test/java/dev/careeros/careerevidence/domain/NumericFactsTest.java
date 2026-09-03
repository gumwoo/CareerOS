package dev.careeros.careerevidence.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumericFactsTest {

    private static final String SOURCE =
            "1000개 연결 환경에서 JavaMonitorEnter가 341에서 0으로 줄었고, 조회는 1.22s에서 426ms가 되었습니다.";

    @Test
    @DisplayName("원문에 있는 수치는 통과한다")
    void acceptsNumbersPresentInSource() {
        assertThat(NumericFacts.allPresentIn("341", SOURCE)).isTrue();
        assertThat(NumericFacts.allPresentIn("0", SOURCE)).isTrue();
        assertThat(NumericFacts.allPresentIn("426ms", SOURCE)).isTrue();
    }

    @Test
    @DisplayName("원문에 없는 수치는 거부한다 — 이 가드의 존재 이유")
    void rejectsFabricatedNumbers() {
        assertThat(NumericFacts.allPresentIn("1400", SOURCE)).isFalse();
        assertThat(NumericFacts.allPresentIn("10000", SOURCE)).isFalse();
        assertThat(NumericFacts.notFoundIn("800ms", SOURCE)).containsExactly("800");
    }

    @Test
    @DisplayName("단위 표기가 달라도 숫자가 같으면 통과한다 — 오탐을 만들지 않는다")
    void ignoresUnitNotation() {
        assertThat(NumericFacts.allPresentIn("341회", SOURCE)).isTrue();
        assertThat(NumericFacts.allPresentIn("약 1000", SOURCE)).isTrue();
    }

    @Test
    @DisplayName("자릿수 쉼표와 소수점 표기를 정규화한다")
    void normalizesFormatting() {
        assertThat(NumericFacts.allPresentIn("1,000", SOURCE)).isTrue();
        assertThat(NumericFacts.allPresentIn("1.220s", SOURCE)).isTrue();
        assertThat(NumericFacts.extract("1,000")).containsExactly("1000");
        assertThat(NumericFacts.extract("44.0")).containsExactly("44");
    }

    @Test
    @DisplayName("수치가 없는 주장은 이 가드의 대상이 아니다")
    void ignoresQualitativeClaims() {
        assertThat(NumericFacts.allPresentIn("크게 개선됨", SOURCE)).isTrue();
        assertThat(NumericFacts.allPresentIn("", SOURCE)).isTrue();
        assertThat(NumericFacts.allPresentIn(null, SOURCE)).isTrue();
    }
}

package dev.careeros.careerevidence.domain;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 텍스트에서 수치를 뽑아 비교한다.
 *
 * <p>존재 이유는 하나다. 다음 실패를 막는 것.
 *
 * <pre>
 * 원문:   JavaMonitorEnter 341 -> 0
 * 추출:   before=1400, after=0
 * </pre>
 *
 * <p>이 값은 DRAFT로 저장되고, 사용자는 자기 경력이므로 "그랬던 것 같다"고 확인한다.
 * 확인된 순간 CONFIRMED가 되어 사실이 되고, evidenceRef가 붙은 채 이력서 문장에 들어간다.
 * 추적 체인은 형식적으로 완전한데 끝점의 숫자가 원문에 없다.
 * 이것이 이 제품이 가장 피해야 할 실패다.
 *
 * <p>비교는 <b>숫자에 대해서만</b> 한다. 단위 표기("14", "14s", "약 14초")는
 * 자유롭게 달라질 수 있고 그것까지 막으면 오탐이 실용성을 해친다.
 */
public final class NumericFacts {

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

    private NumericFacts() {
    }

    /**
     * 텍스트에 등장하는 수치를 정규화해 모은다.
     *
     * <p>자릿수 구분 쉼표는 제거한다. 원문의 {@code 1,000}과 추출된 {@code 1000}은 같은 값이다.
     * 소수점 이하 0은 잘라내 {@code 44.0}과 {@code 44}를 같게 본다.
     */
    public static Set<String> extract(String text) {
        Set<String> values = new LinkedHashSet<>();
        if (text == null) {
            return values;
        }
        Matcher matcher = NUMBER.matcher(text.replace(",", ""));
        while (matcher.find()) {
            values.add(normalize(matcher.group()));
        }
        return values;
    }

    /**
     * {@code claimed}에 등장하는 모든 수치가 {@code source}에도 있는가.
     *
     * <p>수치가 전혀 없는 주장은 이 검사의 대상이 아니므로 통과시킨다.
     * (정성적 서술까지 막는 것은 이 가드의 역할이 아니다)
     */
    public static boolean allPresentIn(String claimed, String source) {
        return extract(source).containsAll(extract(claimed));
    }

    /** {@code claimed}에는 있고 {@code source}에는 없는 수치. 오류 메시지용. */
    public static Set<String> notFoundIn(String claimed, String source) {
        Set<String> missing = new LinkedHashSet<>(extract(claimed));
        missing.removeAll(extract(source));
        return missing;
    }

    private static String normalize(String number) {
        if (!number.contains(".")) {
            return number.replaceFirst("^0+(?=\\d)", "");
        }
        String trimmed = number.replaceFirst("0+$", "").replaceFirst("\\.$", "");
        return trimmed.replaceFirst("^0+(?=\\d)", "");
    }
}

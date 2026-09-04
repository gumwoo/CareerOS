package dev.careeros.careerevidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.careeros.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 첫 vertical slice 전체 흐름.
 *
 * <pre>
 * SourceInput 저장 -> 추출 -> 스키마 검증 -> 원문 대조 -> DRAFT
 *   -> 사용자 확인 -> CONFIRMED -> Career Bank 조회
 * </pre>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("stub")   // 스텁은 이제 기본값이 아니다. 명시해야 등록된다.
class CareerEvidenceFlowIT {

    private static final String RAW_TEXT =
            "다중 SSE 연결에서 최대 14초 지연이 발생했고, JFR로 분석해서 "
                    + "SseEmitter.send()의 락 경합을 찾아 전송 구조를 직렬화했습니다.";

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("원문 등록부터 CONFIRMED 조회까지 한 번에 통과한다")
    void fullFlow() throws Exception {
        MockMvc mvc = mockMvc();

        // 1. 원문 등록 — 추적 체인의 끝
        String sourceResponse = mvc.perform(post("/api/source-inputs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "USER_INPUT", "rawText": "%s" }
                                """.formatted(RAW_TEXT)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sourceInputId = objectMapper.readTree(sourceResponse).get("id").asText();

        // 2. 추출 -> 검증 -> DRAFT. 응답에 원문이 함께 온다(대조용).
        String draftResponse = mvc.perform(post("/api/career-evidences/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sourceInputId": "%s" }
                                """.formatted(sourceInputId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidences").isArray())
                .andExpect(jsonPath("$.evidences[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.evidences[0].code").value(org.hamcrest.Matchers.matchesPattern("CE-\\d{5}")))
                .andExpect(jsonPath("$.source.rawText").value(RAW_TEXT))
                .andReturn().getResponse().getContentAsString();

        JsonNode draft = objectMapper.readTree(draftResponse);
        String evidenceId = draft.at("/evidences/0/id").asText();

        // excerpt는 원문에서 그대로 나온 구절이어야 한다
        String excerpt = draft.at("/evidences/0/source/excerpt").asText();
        assertThat(RAW_TEXT).contains(excerpt);
        assertThat(draft.at("/evidences/0/source/originId").asText()).isEqualTo(sourceInputId);

        // 3. 확인 전에는 Career Bank(CONFIRMED)에 보이지 않는다
        mvc.perform(get("/api/career-evidences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + evidenceId + "')]").isEmpty());

        // 4. 사용자 확인
        mvc.perform(post("/api/career-evidences/{id}/confirm", evidenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 5. 이제 조회된다
        mvc.perform(get("/api/career-evidences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + evidenceId + "')]").isNotEmpty());

        // 6. 두 번 확인할 수 없다
        mvc.perform(post("/api/career-evidences/{id}/confirm", evidenceId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("존재하지 않는 원문으로는 추출할 수 없다")
    void rejectsUnknownSourceInput() throws Exception {
        mockMvc().perform(post("/api/career-evidences/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sourceInputId": "00000000-0000-0000-0000-000000000000" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("빈 원문은 등록되지 않는다")
    void rejectsBlankRawText() throws Exception {
        mockMvc().perform(post("/api/source-inputs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "USER_INPUT", "rawText": "   " }
                                """))
                .andExpect(status().isBadRequest());
    }
}

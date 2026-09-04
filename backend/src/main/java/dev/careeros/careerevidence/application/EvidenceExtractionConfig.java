package dev.careeros.careerevidence.application;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableConfigurationProperties(EvidenceExtractionProperties.class)
public class EvidenceExtractionConfig {

    /**
     * 프롬프트 버전은 설정값이므로 빌더도 설정에서 만든다.
     * 스텁을 쓸 때도 프롬프트 조립은 테스트 대상이라 프로파일과 무관하게 등록한다.
     */
    @Bean
    EvidencePromptBuilder evidencePromptBuilder(EvidenceExtractionProperties properties) {
        return new EvidencePromptBuilder(properties.promptVersion());
    }

    /**
     * {@code ANTHROPIC_API_KEY} 를 환경에서 읽는다. 키가 없으면 여기서 기동이 실패한다.
     *
     * <p>키를 코드나 설정 파일에 두지 않는다. 로컬은 {@code .env}(gitignore 됨),
     * CI 는 실제 호출을 하지 않으므로 {@code stub} 프로파일로 돈다.
     */
    @Bean
    @Profile("!stub")
    AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.fromEnv();
    }
}

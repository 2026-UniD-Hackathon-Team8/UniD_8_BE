package com.unid8.damideo.question;

import com.unid8.damideo.question.QuestionBankSeeder.SeedSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 기동 시 설정된 문제은행 CSV를 DB에 시딩한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "damideo.question-bank", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QuestionSeedRunner implements ApplicationRunner {

    private final QuestionBankProperties properties;
    private final ResourceLoader resourceLoader;
    private final QuestionBankSeeder questionBankSeeder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource resource = resourceLoader.getResource(properties.getLocation());
        SeedSummary summary = questionBankSeeder.seed(resource);
        log.info("문제은행 시딩 완료: location={}, parsed={}, inserted={}, updatedVersions={}, unchanged={}, skipped={}",
                properties.getLocation(),
                summary.totalParsed(),
                summary.inserted(),
                summary.updatedVersions(),
                summary.unchanged(),
                summary.skipped());
    }
}

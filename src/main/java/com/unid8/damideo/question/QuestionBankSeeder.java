package com.unid8.damideo.question;

import com.unid8.damideo.question.QuestionCsvParser.ParseResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문제은행 CSV를 DB에 반영한다.
 * 기존 답안·오답노트가 참조하는 문제 ID를 보존하기 위해, 내용이 변경된 문제는
 * 기존 활성 버전을 비활성화(is_active = false)한 뒤 새 버전을 INSERT한다.
 */
@Service
@RequiredArgsConstructor
public class QuestionBankSeeder {

    private final QuestionCsvParser questionCsvParser;
    private final QuestionRepository questionRepository;

    @Transactional
    public SeedSummary seed(Resource resource) throws IOException {
        if (!resource.exists()) {
            throw new IllegalStateException("문제은행 CSV 리소스를 찾을 수 없습니다: " + resource);
        }

        ParseResult parseResult;
        try (InputStream inputStream = resource.getInputStream()) {
            parseResult = questionCsvParser.parse(inputStream);
        }

        int inserted = 0;
        int updatedVersions = 0;
        int unchanged = 0;

        for (Question candidate : parseResult.questions()) {
            List<Question> activeRows = questionRepository.findAllByNotionIdAndActiveTrue(candidate.getNotionId());
            if (!activeRows.isEmpty()) {
                if (activeRows.size() == 1 && activeRows.get(0).hasSameContent(candidate)) {
                    unchanged++;
                    continue;
                }
                activeRows.forEach(Question::deactivate);
                questionRepository.saveAllAndFlush(activeRows);
                questionRepository.save(candidate);
                updatedVersions++;
                continue;
            }

            if (!candidate.isActive()) {
                Optional<Question> latest = questionRepository.findTopByNotionIdOrderByIdDesc(candidate.getNotionId());
                if (latest.isPresent() && latest.get().hasSameContent(candidate)) {
                    unchanged++;
                    continue;
                }
            }

            questionRepository.save(candidate);
            inserted++;
        }

        return new SeedSummary(
                parseResult.questions().size(),
                inserted,
                updatedVersions,
                unchanged,
                parseResult.skippedRows().size()
        );
    }

    public record SeedSummary(
            int totalParsed,
            int inserted,
            int updatedVersions,
            int unchanged,
            int skipped
    ) {
    }
}

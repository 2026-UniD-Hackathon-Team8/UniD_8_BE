package com.unid8.damideo.question;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 문제은행 CSV 시딩 설정.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "damideo.question-bank")
public class QuestionBankProperties {

    private boolean enabled = true;
    private String location = "classpath:data/문제은행 v1.4.csv";
}

package com.community.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableBatchProcessing  // 배치에 필요한 다양한 설정들을 자동으로 주입
@Configuration
public class TestJobConfig {

    @Bean
    public JobLauncherTestUtils jobLauncherTestUtils() {
    //  배치의 Job을 실행해 테스트하는 유틸리티 클래스를 빈으로 등록
        return new JobLauncherTestUtils();
    }
}

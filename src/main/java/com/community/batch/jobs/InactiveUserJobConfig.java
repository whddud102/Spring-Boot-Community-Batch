package com.community.batch.jobs;

import com.community.batch.domain.User;
import com.community.batch.domain.enums.UserStatus;
import com.community.batch.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 휴면 회원 Job 설정 클래스
 */
@AllArgsConstructor
@Configuration
public class InactiveUserJobConfig {

    private UserRepository userRepository;

    /**
     * 휴면 회원 배치 정보를 빈으로 등록
     * @param jobBuilderFactory Job 생성을 도와주는 BuilderFactory
     * @param inactiveJobStep 휴면 회원 관련 Step 객체
     * @return 휴면 회원 배치 Job
     */
    @Bean
    public Job inactiveUserJobConfig(JobBuilderFactory jobBuilderFactory, Step inactiveJobStep) {
        return jobBuilderFactory.get("inactiveUserJob") // "inactiveUserJob 이라는 JobBuilder 생성
                .start(inactiveJobStep).build();
    }

    @Bean
    public Step inactiveUserJobStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("inactiveUserStep").<User, User> chunk(10)
                .reader(inacUserReader())
                .processor(inactiveUserProcessor())
                .writer(inactiveUserWriter())
                .build();
    }

    @Bean
    @StepScope
    public QueueItemReader<User> inactiveUserReader() {
        List<User> oldUsers = userRepository.findByUpdatedBeforeAndStatusEquls(LocalDateTime.now().minusYears(1), UserStatus.ACTIVE);
        return QueueItemReader<>(oldUsers);
    }


}

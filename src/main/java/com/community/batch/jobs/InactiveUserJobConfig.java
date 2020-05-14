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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 휴면 회원 Job 설정 클래스
 */
@AllArgsConstructor
@Configuration
public class InactiveUserJobConfig {

    private final static int CHUNK_SIZE = 15;
    private final EntityManagerFactory entityManagerFactory;

    private UserRepository userRepository;

    /**
     * 휴면 회원 배치 정보를 빈으로 등록
     * @param jobBuilderFactory Job 생성을 도와주는 BuilderFactory
     * @param inactiveJobStep 휴면 회원 관련 Step 객체
     * @return 휴면 회원 배치 Job
     */
    @Bean
    public Job inactiveUserJob(JobBuilderFactory jobBuilderFactory, Step inactiveJobStep) {
        return jobBuilderFactory.get("inactiveUserJob") // "inactiveUserJob 이라는 JobBuilder 생성
                .preventRestart().start(inactiveJobStep).build();
    }

    @Bean
    public Step inactiveUserJobStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("inactiveUserStep").<User, User> chunk(CHUNK_SIZE)
                .reader(inactiveUserJpaReader())
                .processor(inactiveUserProcessor())
                .writer(inactiveUserWriter())
                .build();
    }

    /**
     * JpaPagingItemReader는 DB에서 원하는 크기 만큼 배치 데이터를 읽어옴
     * @return JpaPaingItemReader
     */
    @Bean(destroyMethod = "")   // destroyedMethod 기능을 사용하지 않도록 설정
    @StepScope
    public JpaPagingItemReader<User> inactiveUserJpaReader() {
        JpaPagingItemReader<User> jpaPagingItemReader = new JpaPagingItemReader<>();
        // 배치 데이터를 가져올 쿼리를 직접 작성해야 함.
        jpaPagingItemReader.setQueryString("select u from User as u where u.updatedDate < :updatedDate and u.status = :status");

        // 쿼리에 사용할 파라미터의 값을 Map에 지정
        Map<String, Object> map = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        map.put("updatedDate", now.minusYears(1));
        map.put("status", UserStatus.ACTIVE);

        // 쿼리에 사용할 파라미터를 등록
        jpaPagingItemReader.setParameterValues(map);
        jpaPagingItemReader.setEntityManagerFactory(entityManagerFactory);
        jpaPagingItemReader.setPageSize(CHUNK_SIZE);    // 한 번에 가져올 개수를 지정

        return jpaPagingItemReader;
    }

    /**
     * Chunk 단위로 아이템을 받아서 DB에 저장하는 ItemWriter 를 반환
     * @return JpaItemWriter
     */
    private JpaItemWriter<User> inactiveUserWriter() {
        JpaItemWriter<User> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
        return jpaItemWriter;
    }


    /**
     * 배치 처리의 비즈니스 로직을 담당하는 ItemProcessor 를 반환하는 메서드
     * @return ItemProcessor
     */
    private ItemProcessor<User, User> inactiveUserProcessor() {
        return new ItemProcessor<User, User>() {
            @Override
            public User process(User item) throws Exception {
                return item.setInactive();     // User 객체를 휴면 상태로 전환
            }
        };
    }

    @Bean
    @StepScope
    public ListItemReader<User> inactiveUserReader() {
        // ListItemReader 는 데이터를 한번에 가져와 메모리에 올려놓고, read() 메서드로 하나씩 읽어와서 배치 처리 수행
        List<User> oldUsers = userRepository.findByUpdatedDateBeforeAndStatusEquals(LocalDateTime.now().minusYears(1), UserStatus.ACTIVE);
        return new ListItemReader<>(oldUsers);
    }


}

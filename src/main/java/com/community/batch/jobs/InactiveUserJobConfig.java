package com.community.batch.jobs;

import com.community.batch.domain.User;
import com.community.batch.domain.enums.UserStatus;
import com.community.batch.jobs.inactive.InactiveJobExecutionDecider;
import com.community.batch.jobs.inactive.listener.InactiveJobListener;
import com.community.batch.jobs.inactive.listener.InactiveStepListener;
import com.community.batch.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 휴면 회원 Job 설정 클래스
 */
@AllArgsConstructor
@Configuration
public class InactiveUserJobConfig {

    private final static int CHUNK_SIZE = 5;
    private final EntityManagerFactory entityManagerFactory;

    private UserRepository userRepository;

    /**
     * 휴면 회원 배치 정보를 빈으로 등록
     * @param jobBuilderFactory Job 생성을 도와주는 BuilderFactory
     * @param inactiveJobStep 휴면 회원 관련 Step 객체
     * @return 휴면 회원 배치 Job
     */
    @Bean
    public Job inactiveUserJob(
            JobBuilderFactory jobBuilderFactory,
            Step inactiveJobStep,
            InactiveJobListener inactiveJobListener,
            Flow inactiveJobFlow) {
        return jobBuilderFactory.get("inactiveUserJob") // "inactiveUserJob 이라는 JobBuilder 생성
                .preventRestart()
                .listener(inactiveJobListener)  // Job Execution Listener 를 등록
                .start(inactiveJobFlow).end()   // Flow를 거쳐 Step을 실행하도록 설정
                .build();
    }

    @Bean
    public Flow inactiveJobFlow(Step inactiveJobStep) {
        FlowBuilder<Flow> flowBuilder = new FlowBuilder<>("inactiveJobFlow");

        return flowBuilder
                .start(new InactiveJobExecutionDecider())   // InactiveJobExecutionDecider를 가장 먼저 실행하도록 설정
                .on(FlowExecutionStatus.FAILED.getName()).end() // FlowExecutionDecider의 결과가 FAILED 이면 end()로 바로 종료
                .on(FlowExecutionStatus.COMPLETED.getName()).to(inactiveJobStep)    // FlowExecutionDecider의 결과가 COMPLETED 이면 step 실행
                .end();
    }

    @Bean
    public Step inactiveJobStep(
            StepBuilderFactory stepBuilderFactory,
            ListItemReader<User> inactiveUserReader,
            InactiveStepListener inactiveStepListener,
            TaskExecutor taskExecutor) {
        return stepBuilderFactory.get("inactiveUserStep").<User, User> chunk(CHUNK_SIZE)
                .reader(inactiveUserReader)
                .processor(inactiveUserProcessor())
                .writer(inactiveUserWriter())
                .listener(inactiveStepListener) // Step Listener를 등록
                .taskExecutor(taskExecutor)     // taskExecutor 객체를 등록해서 멀티 스레드로 Step을 여러개 수행
                .throttleLimit(2)       // 스레드를 동시에 실행시킬 최대 개수
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        // 스레드를 요청할 때 마다 새로운 스레드를 생성하는 SimpleAsnyTaskExecutor 객체를 빈으로 등록
        return new SimpleAsyncTaskExecutor("Batch_Task");
    }


    /**
     * JpaPagingItemReader는 DB에서 원하는 크기 만큼 배치 데이터를 읽어옴
     * @return JpaPaingItemReader
     */
    /*
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
    } */



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

    /**
     * @param nowDate Job Parameters를 통해 전달한 기준 시각 데이터
     * @param userRepository User Repository
     * @return ListItemReader
     */
    @Bean
    @StepScope
    public ListItemReader<User> inactiveUserReader(@Value("#{jobParameters[nowDate]}") Date nowDate, UserRepository userRepository) {
        // Job Parameters를 통해 전달한 기준 시각 데이터를 가져와서 LocalDateTime 으로 변환
        LocalDateTime now = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault());
        List<User> inactiveUsers = userRepository.findByUpdatedDateBeforeAndStatusEquals(now.minusYears(1), UserStatus.ACTIVE);

        return new ListItemReader<>(inactiveUsers);
    }


}

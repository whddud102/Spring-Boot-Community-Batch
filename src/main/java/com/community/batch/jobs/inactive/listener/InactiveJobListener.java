package com.community.batch.jobs.inactive.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j      // 로그 객체 사용을 위한 어노테이션
@Component  // 외부에서 주입 받을 수 있도록 스프링 빈으로 등록
public class InactiveJobListener implements JobExecutionListener {
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Before Job");
        System.out.println("Before Job");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("After Job");
        System.out.println("After Job");
    }
}


package com.duoc.Backen3.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchInfrastructureConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public BatchInfrastructureConfig(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Job exampleJob(Step exampleStep) {
        return new JobBuilder("exampleJob", jobRepository)
                .start(exampleStep)
                .build();
    }

    @Bean
    public Step exampleStep(Tasklet exampleTasklet) {
        return new StepBuilder("exampleStep", jobRepository)
                .tasklet(exampleTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet exampleTasklet() {
        return (contribution, chunkContext) -> RepeatStatus.FINISHED;
    }
}

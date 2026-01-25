package com.duoc.Backen3.jobs;

import com.duoc.Backen3.config.FilePathsProperties;
import com.duoc.Backen3.domain.Account;
import com.duoc.Backen3.domain.InterestResult;
import com.duoc.Backen3.listeners.JobLoggerListener;
import com.duoc.Backen3.processors.AccountInterestProcessor;
import com.duoc.Backen3.support.BatchSkipListener;
import com.duoc.Backen3.support.CsvFieldSetMappers;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.Year;

@Configuration
public class QuarterlyInterestJobConfig {

    private final FilePathsProperties props;
    private final ResourceLoader resourceLoader;

    public QuarterlyInterestJobConfig(FilePathsProperties props, ResourceLoader resourceLoader) {
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Job quarterlyInterestJob(
        JobRepository jobRepository,
        Step quarterlyInterestStep,
        JobLoggerListener listener 
    ) {
        return new JobBuilder("quarterlyInterestJob", jobRepository)
            .start(quarterlyInterestStep)
            .listener(listener) 
            .build();
    }

    @Bean
    public Step quarterlyInterestStep(
            JobRepository jobRepository,
            PlatformTransactionManager txManager,
            FlatFileItemReader<Account> accountsReader,
            JdbcBatchItemWriter<InterestResult> interestWriter,
            JdbcBatchItemWriter<InterestResult> accountBalanceWriter,
            TaskExecutor interestTaskExecutor 
    ) {
        String quarter = Year.now().getValue() + "Q1";

        return new StepBuilder("quarterlyInterestStep", jobRepository)
                .<Account, InterestResult>chunk(5, txManager)
                .reader(accountsReader)
                .processor(new AccountInterestProcessor(quarter))
                .writer(items -> {
                    interestWriter.write(items);
                    accountBalanceWriter.write(items);
                })
                .faultTolerant()
                .skip(IllegalArgumentException.class)
                .skipLimit(500)
                .listener(new BatchSkipListener<Account, InterestResult>())
                .taskExecutor(interestTaskExecutor)
                .throttleLimit(3)
                .build();
    }

    // --- Optimización (Criterio 6) ---
    @Bean
    public TaskExecutor interestTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setThreadNamePrefix("InterestThread-");
        executor.initialize();
        return executor;
    }

    @Bean
    public FlatFileItemReader<Account> accountsReader() {
        FlatFileItemReader<Account> reader = new FlatFileItemReader<>();
        reader.setResource(resourceLoader.getResource(props.getFiles().getAccounts()));
        reader.setLinesToSkip(1);

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("account_id", "account_type", "balance", "annual_rate");

        DefaultLineMapper<Account> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(CsvFieldSetMappers.accountMapper());

        reader.setLineMapper(lineMapper);
        return reader;
    }

    @Bean
    public JdbcBatchItemWriter<InterestResult> interestWriter(DataSource ds) {
        return new JdbcBatchItemWriterBuilder<InterestResult>()
                .dataSource(ds)
                .sql("INSERT INTO interest_results (account_id, quarter, interest_applied, new_balance) " +
                     "VALUES (:accountId, :quarter, :interestApplied, :newBalance)")
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<InterestResult> accountBalanceWriter(DataSource ds) {
        return new JdbcBatchItemWriterBuilder<InterestResult>()
                .dataSource(ds)
                .sql("UPDATE accounts SET balance = :newBalance WHERE account_id = :accountId")
                .beanMapped()
                .build();
    }


}
package com.duoc.Backen3.jobs;

import com.duoc.Backen3.config.FilePathsProperties;
import com.duoc.Backen3.domain.AnnualStatement;
import com.duoc.Backen3.listeners.JobLoggerListener;
import com.duoc.Backen3.processors.AnnualStatementProcessor;
import com.duoc.Backen3.support.BatchSkipListener;
import com.duoc.Backen3.support.CsvFieldSetMappers;
import com.duoc.Backen3.tasklets.AnnualReportTasklet;

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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class AnnualStatementsJobConfig {

    private final FilePathsProperties props;
    private final ResourceLoader resourceLoader;

    public AnnualStatementsJobConfig(FilePathsProperties props, ResourceLoader resourceLoader) {
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Job annualStatementsJob(
        JobRepository jobRepository,
        Step annualStatementsStep,
        Step annualReportStep,
        JobLoggerListener listener // 1. Inyectas el listener aquí
    ) {
        return new JobBuilder("annualStatementsJob", jobRepository)
            .start(annualStatementsStep)
            .next(annualReportStep)
            .listener(listener) // 2. Lo conectas al Job
            .build();
    }

    @Bean
    public Step annualStatementsStep(JobRepository jobRepository,
                                     PlatformTransactionManager txManager,
                                     FlatFileItemReader<AnnualStatement> annualReader,
                                     JdbcBatchItemWriter<AnnualStatement> annualWriter,
                                     TaskExecutor annualTaskExecutor) {

        return new StepBuilder("annualStatementsStep", jobRepository)
                .<AnnualStatement, AnnualStatement>chunk(5, txManager)
                .reader(annualReader)
                .processor(new AnnualStatementProcessor())
                .writer(annualWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(500)
                .listener(new BatchSkipListener<AnnualStatement, AnnualStatement>())
                .taskExecutor(annualTaskExecutor)
                .throttleLimit(3)
                .build();
    }

    // --- Optimización: Executor dedicado para el Job Anual ---
    @Bean
    public TaskExecutor annualTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setThreadNamePrefix("Annual-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Step annualReportStep(JobRepository jobRepository,
                                 PlatformTransactionManager txManager,
                                 NamedParameterJdbcTemplate jdbcTemplate) {
        return new StepBuilder("annualReportStep", jobRepository)
                .tasklet(new AnnualReportTasklet(
                        jdbcTemplate,
                        props.getOutput().getAnnualReport()
                ), txManager)
                .build();
    }

    @Bean
    public FlatFileItemReader<AnnualStatement> annualReader() {
        FlatFileItemReader<AnnualStatement> reader = new FlatFileItemReader<>();
        reader.setResource(resourceLoader.getResource(props.getFiles().getAnnualStatements()));
        reader.setLinesToSkip(1);

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("account_id", "year", "total_credits", "total_debits", "ending_balance");

        DefaultLineMapper<AnnualStatement> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(CsvFieldSetMappers.annualMapper());

        reader.setLineMapper(lineMapper);
        return reader;
    }

    @Bean
    public JdbcBatchItemWriter<AnnualStatement> annualWriter(DataSource ds) {
        return new JdbcBatchItemWriterBuilder<AnnualStatement>()
                .dataSource(ds)
                .sql("INSERT INTO annual_statements (account_id, year, total_credits, total_debits, ending_balance, audit_flag, audit_note) " +
                     "VALUES (:accountId, :year, :totalCredits, :totalDebits, :endingBalance, :auditFlag, :auditNote)")
                .beanMapped()
                .build();
    }

    
}
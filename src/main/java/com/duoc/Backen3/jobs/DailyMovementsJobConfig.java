package com.duoc.Backen3.jobs;

import com.duoc.Backen3.config.FilePathsProperties;
import com.duoc.Backen3.domain.DailyTransaction;
import com.duoc.Backen3.listeners.JobLoggerListener;
import com.duoc.Backen3.processors.DailyTransactionProcessor;
import com.duoc.Backen3.support.BatchSkipListener;
import com.duoc.Backen3.support.CsvFieldSetMappers;
import com.duoc.Backen3.tasklets.DailySummaryTasklet;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor; // Importante
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class DailyMovementsJobConfig {

    private final FilePathsProperties props;
    private final ResourceLoader resourceLoader;

    public DailyMovementsJobConfig(FilePathsProperties props, ResourceLoader resourceLoader) {
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Job dailyMovementsJob(
        JobRepository jobRepository,
        Step dailyMovementsStep,
        JobLoggerListener listener // Inyectas el mismo
    ) {
        return new JobBuilder("dailyMovementsJob", jobRepository)
            .start(dailyMovementsStep)
            .listener(listener) // Lo conectas
            .build();
    }

    @Bean
    public Step dailyMovementsStep(JobRepository jobRepository,
                                   PlatformTransactionManager txManager,
                                   FlatFileItemReader<DailyTransaction> dailyTxReader,
                                   JdbcBatchItemWriter<DailyTransaction> dailyTxWriter,
                                   TaskExecutor taskExecutor) {

        return new StepBuilder("dailyMovementsStep", jobRepository)
                .<DailyTransaction, DailyTransaction>chunk(5, txManager)
                .reader(dailyTxReader)
                .processor(new DailyTransactionProcessor()) // Usamos el procesador manual
                .writer(dailyTxWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .listener(new BatchSkipListener<DailyTransaction, DailyTransaction>())
                .taskExecutor(taskExecutor) // Multihilo activo
                .build();
    }

    // Bean de Optimización (Criterio 6)
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setThreadNamePrefix("DailyJob-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Step dailySummaryStep(JobRepository jobRepository,
                                 PlatformTransactionManager txManager,
                                 NamedParameterJdbcTemplate jdbcTemplate) {
        return new StepBuilder("dailySummaryStep", jobRepository)
                .tasklet(new DailySummaryTasklet(
                        jdbcTemplate,
                        props.getOutput().getDailySummary()
                ), txManager)
                .build();
    }

    @Bean
    public FlatFileItemReader<DailyTransaction> dailyMovementsReader() {
        return new FlatFileItemReaderBuilder<DailyTransaction>()
                .name("dailyMovementsReader")
                // 1. Ruta corregida con subcarpeta 'data'
                .resource(new ClassPathResource("data/daily_transactions.csv"))
                // 2. Saltar encabezados para evitar el error de la línea 1
                .linesToSkip(1)
                .delimited()
                // 3. Mapeo de nombres exacto a tu clase DailyTransaction
                .names("txId", "accountId", "txTimestamp", "amount", "channel")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<DailyTransaction>() {{
                    setTargetType(DailyTransaction.class);
                    // 4. El "Traductor" de fechas para procesar la 'T' del ISO
                    setCustomEditors(java.util.Collections.singletonMap(
                        java.time.LocalDateTime.class, 
                        new java.beans.PropertyEditorSupport() {
                            @Override
                            public void setAsText(String text) {
                                // Convierte "2026-01-24T10:00:00" en un objeto Java real
                                setValue(java.time.LocalDateTime.parse(text));
                            }
                        }
                    ));
                }})
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<DailyTransaction> dailyTxWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<DailyTransaction>()
                .dataSource(dataSource)
                .sql("INSERT INTO daily_transactions (tx_id, account_id, tx_timestamp, amount, channel, anomaly, anomaly_reason) " +
                    "VALUES (:txId, :accountId, :txTimestamp, :amount, :channel, :anomaly, :anomalyReason)")
                .beanMapped()
                .build();
    }

    
}
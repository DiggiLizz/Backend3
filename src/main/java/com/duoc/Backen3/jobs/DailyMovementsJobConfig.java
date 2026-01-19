
package com.duoc.Backen3.jobs;

import com.duoc.Backen3.config.FilePathsProperties;
import com.duoc.Backen3.domain.DailyTransaction;
import com.duoc.Backen3.processors.DailyTransactionProcessor;
import com.duoc.Backen3.support.BatchSkipListener;
import com.duoc.Backen3.support.CsvFieldSetMappers;
import com.duoc.Backen3.tasklets.DailySummaryTasklet;

import lombok.RequiredArgsConstructor;

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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class DailyMovementsJobConfig {

    // propiedades con rutas de archivos
    private final FilePathsProperties props;

    // cargador de recursos para acceder a csv
    private final ResourceLoader resourceLoader;

    // ----------------------- JOB -----------------------
    @Bean
    public Job dailyMovementsJob(JobRepository jobRepository,
                                 Step dailyMovementsStep,
                                 Step dailySummaryStep) {
        return new JobBuilder("dailyMovementsJob", jobRepository)
                .start(dailyMovementsStep)   // primero procesa
                .next(dailySummaryStep)      // luego genera el resumen
                .build();
    }

    // ----------------------- STEP CHUNK -----------------------
    @Bean
    public Step dailyMovementsStep(JobRepository jobRepository,
                                   PlatformTransactionManager txManager,
                                   FlatFileItemReader<DailyTransaction> dailyTxReader,
                                   JdbcBatchItemWriter<DailyTransaction> dailyTxWriter,
                                   TaskExecutor taskExecutor) {

        return new StepBuilder("dailyMovementsStep", jobRepository)
                .<DailyTransaction, DailyTransaction>chunk(5, txManager) // requisito: chunk de 5
                .reader(dailyTxReader)
                .processor(new DailyTransactionProcessor())
                .writer(dailyTxWriter)
                .faultTolerant()
                .skip(IllegalArgumentException.class)    // omite validaciones/mapeos inválidos
                .skipLimit(1000)
                .retry(Exception.class)                  // ✅ OBLIGATORIO si usas retryLimit
                .retryLimit(3)                           // reintenta 3 veces errores transitorios
                .listener(new BatchSkipListener<DailyTransaction, DailyTransaction>())
                .taskExecutor(taskExecutor)              // multihilo
                .throttleLimit(3)                        // 3 hilos (pauta)
                .build();
    }

    // ----------------------- STEP TASKLET (resumen) -----------------------
    @Bean
    public Step dailySummaryStep(JobRepository jobRepository,
                                 PlatformTransactionManager txManager,
                                 NamedParameterJdbcTemplate jdbcTemplate) {
        return new StepBuilder("dailySummaryStep", jobRepository)
                .tasklet(new DailySummaryTasklet(jdbcTemplate, props.getOutput().getDailySummary()), txManager)
                .build();
    }

    // ----------------------- READER CSV -----------------------
    @Bean
    public FlatFileItemReader<DailyTransaction> dailyTxReader() {
        FlatFileItemReader<DailyTransaction> reader = new FlatFileItemReader<>();
        reader.setResource(resourceLoader.getResource(props.getFiles().getDailyTransactions()));
        reader.setLinesToSkip(1);

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames("tx_id", "account_id", "tx_timestamp", "amount", "channel");

        DefaultLineMapper<DailyTransaction> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(CsvFieldSetMappers.dailyTxMapper());

        reader.setLineMapper(lineMapper);
        return reader;
    }

    // ----------------------- WRITER JDBC -----------------------
    @Bean
    public JdbcBatchItemWriter<DailyTransaction> dailyTxWriter(DataSource dataSource) {
        // builder recomendado en Spring Batch 5
        return new JdbcBatchItemWriterBuilder<DailyTransaction>()
                .dataSource(dataSource)
                .sql("""
                     INSERT INTO daily_transactions
                     (tx_id, account_id, tx_timestamp, amount, channel, anomaly, anomaly_reason)
                     VALUES
                     (:txId, :accountId, :txTimestamp, :amount, :channel, :anomaly, :anomalyReason)
                     """)
                .beanMapped() // usa BeanPropertyItemSqlParameterSourceProvider internamente
                .build();
    }
}
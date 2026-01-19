
package com.duoc.Backen3.jobs;

import com.duoc.Backen3.config.FilePathsProperties;
import com.duoc.Backen3.domain.DailyTransaction;
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
public class DailyMovementsJobConfig {

    // rutas y salidas configurables (finabc.*)
    private final FilePathsProperties props;

    // para cargar recursos del classpath/sistema
    private final ResourceLoader resourceLoader;

    // Constructor explícito (evita depender de Lombok)
    public DailyMovementsJobConfig(FilePathsProperties props, ResourceLoader resourceLoader) {
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    // ----------------------- JOB -----------------------
    @Bean
    public Job dailyMovementsJob(JobRepository jobRepository,
                                 Step dailyMovementsStep,
                                 Step dailySummaryStep) {
        return new JobBuilder("dailyMovementsJob", jobRepository)
                .incrementer(new RunIdIncrementer())  // ejecuciones únicas con run.id
                .start(dailyMovementsStep)             // 1) procesa CSV -> DB
                .next(dailySummaryStep)                // 2) genera resumen (tasklet)
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
                .<DailyTransaction, DailyTransaction>chunk(5, txManager) // chunk de 5
                .reader(dailyTxReader)
                .processor(new DailyTransactionProcessor())
                .writer(dailyTxWriter)
                .faultTolerant()
                .skip(IllegalArgumentException.class)   // omite mapeos/validaciones inválidas
                .skipLimit(1000)
                .retry(Exception.class)                 // requerido si usas retryLimit
                .retryLimit(3)                          // 3 reintentos a fallas transitorias
                .listener(new BatchSkipListener<DailyTransaction, DailyTransaction>())
                .taskExecutor(taskExecutor)             // multihilo
                .throttleLimit(3)                       // máximo 3 hilos
                .build();
    }

    // ----------------------- STEP TASKLET (resumen) -----------------------
    @Bean
    public Step dailySummaryStep(JobRepository jobRepository,
                                 PlatformTransactionManager txManager,
                                 NamedParameterJdbcTemplate jdbcTemplate) {
        return new StepBuilder("dailySummaryStep", jobRepository)
                .tasklet(new DailySummaryTasklet(
                        jdbcTemplate,
                        props.getOutput().getDailySummary() // ruta de salida
                ), txManager)
                .build();
    }

    // ----------------------- READER CSV -----------------------
    @Bean
    public FlatFileItemReader<DailyTransaction> dailyTxReader() {
        FlatFileItemReader<DailyTransaction> reader = new FlatFileItemReader<>();
        // Soporta classpath: o file:
        reader.setResource(resourceLoader.getResource(props.getFiles().getDailyTransactions()));
        reader.setLinesToSkip(1); // omite header

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames("tx_id", "account_id", "tx_timestamp", "amount", "channel");
        tokenizer.setStrict(false); // tolerante ante columnas extra/menos

        DefaultLineMapper<DailyTransaction> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(CsvFieldSetMappers.dailyTxMapper());

        reader.setLineMapper(lineMapper);
        return reader;
    }

    // ----------------------- WRITER JDBC -----------------------
    @Bean
    public JdbcBatchItemWriter<DailyTransaction> dailyTxWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<DailyTransaction>()
                .dataSource(dataSource)
                .sql(
                    "INSERT INTO daily_transactions " +
                    "(tx_id, account_id, tx_timestamp, amount, channel, anomaly, anomaly_reason) " +
                    "VALUES (:txId, :accountId, :txTimestamp, :amount, :channel, :anomaly, :anomalyReason)"
                )
                .beanMapped() // mapea propiedades del bean a parámetros SQL
                .build();
    }
}

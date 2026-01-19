
package com.duoc.Backen3.jobs;

import com.duoc.Backen3.config.FilePathsProperties;
import com.duoc.Backen3.domain.Account;
import com.duoc.Backen3.domain.InterestResult;
import com.duoc.Backen3.processors.AccountInterestProcessor;
import com.duoc.Backen3.support.BatchSkipListener;
import com.duoc.Backen3.support.CsvFieldSetMappers;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.Year;

@Configuration
public class QuarterlyInterestJobConfig {

    // propiedades con rutas de archivos csv
    private final FilePathsProperties props;

    // cargador de recursos para acceder a archivos csv
    private final ResourceLoader resourceLoader;

    // ---- Constructor explícito (reemplaza @RequiredArgsConstructor) ----
    public QuarterlyInterestJobConfig(FilePathsProperties props, ResourceLoader resourceLoader) {
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    // ----------------------- JOB -----------------------
    @Bean
    public Job quarterlyInterestJob(
            JobRepository jobRepository,
            Step quarterlyInterestStep
    ) {
        return new JobBuilder("quarterlyInterestJob", jobRepository)
                .start(quarterlyInterestStep)
                .build();
    }

    // --------------- STEP principal (multithread) ---------------
    @Bean
    public Step quarterlyInterestStep(
            JobRepository jobRepository,
            PlatformTransactionManager txManager,
            FlatFileItemReader<Account> accountsReader,
            JdbcBatchItemWriter<InterestResult> interestWriter,
            JdbcBatchItemWriter<InterestResult> accountBalanceWriter,
            TaskExecutor taskExecutor
    ) {

        // definición del trimestre actual para el cálculo de intereses (ajústalo si usas otro Q)
        String quarter = Year.now().getValue() + "Q1";

        return new StepBuilder("quarterlyInterestStep", jobRepository)
                // procesamiento por bloques de 5 registros
                .<Account, InterestResult>chunk(5, txManager)
                // lector de cuentas desde csv
                .reader(accountsReader)
                // processor que calcula interés y nuevo saldo
                .processor(new AccountInterestProcessor(quarter))
                // escritura doble: tabla de resultados y actualización de saldo
                .writer(items -> {
                    interestWriter.write(items);
                    accountBalanceWriter.write(items);
                })
                // tolerancia a fallos
                .faultTolerant()
                .skip(IllegalArgumentException.class)
                .skipLimit(500)
                .listener(new BatchSkipListener<Account, InterestResult>())
                // multihilo
                .taskExecutor(taskExecutor)
                .throttleLimit(3)
                .build();
    }

    // ----------------------- READER CSV -----------------------
    @Bean
    public FlatFileItemReader<Account> accountsReader() {
        FlatFileItemReader<Account> reader = new FlatFileItemReader<>();

        // ruta del archivo csv de cuentas (classpath: o file:)
        reader.setResource(
                resourceLoader.getResource(
                        props.getFiles().getAccounts()
                )
        );

        // omite encabezado del csv
        reader.setLinesToSkip(1);

        // tokenizador por coma con nombres de columnas
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames("account_id", "account_type", "balance", "annual_rate");
        tokenizer.setStrict(false); // tolerante a columnas extra/menos u orden leve

        // mapeo de línea csv a objeto Account
        DefaultLineMapper<Account> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(CsvFieldSetMappers.accountMapper());

        reader.setLineMapper(lineMapper);
        return reader;
    }

    // -------- writer jdbc: insertar resultados de intereses --------
    @Bean
    public JdbcBatchItemWriter<InterestResult> interestWriter(DataSource ds) {
        JdbcBatchItemWriter<InterestResult> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(ds);
        writer.setSql("""
            INSERT INTO interest_results
            (account_id, quarter, interest_applied, new_balance)
            VALUES
            (:accountId, :quarter, :interestApplied, :newBalance)
        """);
        writer.setItemSqlParameterSourceProvider(
                org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource::new
        );
        return writer;
    }

    // -------- writer jdbc: actualizar el saldo de las cuentas --------
    @Bean
    public JdbcBatchItemWriter<InterestResult> accountBalanceWriter(DataSource ds) {
        JdbcBatchItemWriter<InterestResult> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(ds);
        writer.setSql("""
            UPDATE accounts
            SET balance = :newBalance
            WHERE account_id = :accountId
        """);
        writer.setItemSqlParameterSourceProvider(
                org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource::new
        );
        return writer;
    }
}


package com.duoc.Backen3.jobs;

import com.duoc.Backen3.config.FilePathsProperties;
import com.duoc.Backen3.domain.AnnualStatement;
import com.duoc.Backen3.processors.AnnualStatementProcessor;
import com.duoc.Backen3.support.BatchSkipListener;
import com.duoc.Backen3.support.CsvFieldSetMappers;
import com.duoc.Backen3.tasklets.AnnualReportTasklet;

import lombok.RequiredArgsConstructor;

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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class AnnualStatementsJobConfig {

    private final FilePathsProperties props;

    // bean de spring para cargar recursos
    private final ResourceLoader resourceLoader;

    // bean que define el job anual (procesa y luego genera reporte)
    @Bean
    public Job annualStatementsJob(JobRepository jobRepository,
                                   Step annualStatementsStep,
                                   Step annualReportStep) {
        return new JobBuilder("annualStatementsJob", jobRepository)
            .start(annualStatementsStep)
            .next(annualReportStep)
            .build();
    }

    // bean que define el step chunk para leer y escribir (multithread)
    @Bean
    public Step annualStatementsStep(JobRepository jobRepository,
                                     PlatformTransactionManager txManager,
                                     FlatFileItemReader<AnnualStatement> annualReader,
                                     JdbcBatchItemWriter<AnnualStatement> annualWriter,
                                     TaskExecutor taskExecutor) {             // <-- NUEVO: se inyecta el executor
        return new StepBuilder("annualStatementsStep", jobRepository)
            .<AnnualStatement, AnnualStatement>chunk(5, txManager)           // <-- cambiado a 5 (requisito Semana 2)
            .reader(annualReader)                                            // reader csv
            .processor(new AnnualStatementProcessor())                       // processor con reglas de auditoría
            .writer(annualWriter)                                            // escribe los items procesados
            .faultTolerant()                                                 // tolerancia a fallos
            .skip(IllegalArgumentException.class)                            // omite errores de mapeo/validación
            .skipLimit(500)                                                  // omite hasta 500 errores
            .listener(new BatchSkipListener<AnnualStatement, AnnualStatement>()) // listener items omitidos
            // --------- MULTITHREADING DEL STEP ----------
            .taskExecutor(taskExecutor)                                      // ejecuta chunks en paralelo
            .throttleLimit(3)                                                // usa 3 hilos
            // -------------------------------------------
            .build();
    }

    // bean que define el step tasklet para el reporte anual
    @Bean
    public Step annualReportStep(JobRepository jobRepository,
                                 PlatformTransactionManager txManager,
                                 NamedParameterJdbcTemplate jdbcTemplate) {
        return new StepBuilder("annualReportStep", jobRepository)
            .tasklet(new AnnualReportTasklet(jdbcTemplate, props.getOutput().getAnnualReport()), txManager)
            .build();
    }

    // bean que define el reader del archivo CSV
    @Bean
    public FlatFileItemReader<AnnualStatement> annualReader() {
        FlatFileItemReader<AnnualStatement> reader = new FlatFileItemReader<>();

        // define el recurso CSV desde application properties
        reader.setResource(resourceLoader.getResource(props.getFiles().getAnnualStatements()));

        // omite el header del CSV
        reader.setLinesToSkip(1);

        // tokenizador por coma con nombres de columnas esperados
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames("account_id", "year", "total_credits", "total_debits", "ending_balance");

        // mapeo línea a objeto AnnualStatement
        DefaultLineMapper<AnnualStatement> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(CsvFieldSetMappers.annualMapper());

        reader.setLineMapper(lineMapper);
        return reader;
    }

    // bean que define el writer JDBC para persistir AnnualStatement
    @Bean
    public JdbcBatchItemWriter<AnnualStatement> annualWriter(DataSource ds) {
        JdbcBatchItemWriter<AnnualStatement> writer = new JdbcBatchItemWriter<>();

        // configura el datasource
        writer.setDataSource(ds);

        // SQL de inserción con parámetros nombrados
        writer.setSql("""
            INSERT INTO annual_statements(account_id, year, total_credits, total_debits, ending_balance, audit_flag, audit_note)
            VALUES (:accountId, :year, :totalCredits, :totalDebits, :endingBalance, :auditFlag, :auditNote)
        """);

        // mapea propiedades del objeto a parámetros SQL
        writer.setItemSqlParameterSourceProvider(
            org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource::new
        );

        return writer;
    }
}


package com.duoc.Backen3.jobs;

import com.duoc.Backen3.config.FilePathsProperties;
import com.duoc.Backen3.domain.AnnualStatement;
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
public class AnnualStatementsJobConfig {

    private final FilePathsProperties props;
    private final ResourceLoader resourceLoader;

    // ---- Constructor explícito (reemplaza @RequiredArgsConstructor) ----
    public AnnualStatementsJobConfig(FilePathsProperties props, ResourceLoader resourceLoader) {
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    // ----------------------- JOB -----------------------
    @Bean
    public Job annualStatementsJob(JobRepository jobRepository,
                                   Step annualStatementsStep,
                                   Step annualReportStep) {
        return new JobBuilder("annualStatementsJob", jobRepository)
                .start(annualStatementsStep)
                .next(annualReportStep)
                .build();
    }

    // ----------------------- STEP CHUNK -----------------------
    @Bean
    public Step annualStatementsStep(JobRepository jobRepository,
                                     PlatformTransactionManager txManager,
                                     FlatFileItemReader<AnnualStatement> annualReader,
                                     JdbcBatchItemWriter<AnnualStatement> annualWriter,
                                     TaskExecutor taskExecutor) {

        return new StepBuilder("annualStatementsStep", jobRepository)
                .<AnnualStatement, AnnualStatement>chunk(5, txManager) // requisito: chunk=5
                .reader(annualReader)                                   // reader csv
                .processor(new AnnualStatementProcessor())              // reglas de auditoría
                .writer(annualWriter)                                   // persiste items
                .faultTolerant()
                .skip(IllegalArgumentException.class)                   // omite mapeo/validación inválidos
                .skipLimit(500)
                .listener(new BatchSkipListener<AnnualStatement, AnnualStatement>())
                // --------- MULTITHREADING DEL STEP ----------
                .taskExecutor(taskExecutor)
                .throttleLimit(3)
                // -------------------------------------------
                .build();
    }

    // ----------------------- STEP TASKLET (reporte anual) -----------------------
    @Bean
    public Step annualReportStep(JobRepository jobRepository,
                                 PlatformTransactionManager txManager,
                                 NamedParameterJdbcTemplate jdbcTemplate) {
        return new StepBuilder("annualReportStep", jobRepository)
                .tasklet(new AnnualReportTasklet(
                        jdbcTemplate,
                        props.getOutput().getAnnualReport() // ruta de salida configurada
                ), txManager)
                .build();
    }

    // ----------------------- READER CSV -----------------------
    @Bean
    public FlatFileItemReader<AnnualStatement> annualReader() {
        FlatFileItemReader<AnnualStatement> reader = new FlatFileItemReader<>();

        // define el recurso CSV desde application properties (classpath: o file:)
        reader.setResource(resourceLoader.getResource(props.getFiles().getAnnualStatements()));

        // omite el header del CSV
        reader.setLinesToSkip(1);

        // tokenizador por coma con nombres de columnas esperados
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames("account_id", "year", "total_credits", "total_debits", "ending_balance");
        tokenizer.setStrict(false); // tolera columnas extra/menos/espacios

        // mapeo línea a objeto AnnualStatement
        DefaultLineMapper<AnnualStatement> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(CsvFieldSetMappers.annualMapper());

        reader.setLineMapper(lineMapper);
        return reader;
    }

    // ----------------------- WRITER JDBC -----------------------
    @Bean
    public JdbcBatchItemWriter<AnnualStatement> annualWriter(DataSource ds) {
        JdbcBatchItemWriter<AnnualStatement> writer = new JdbcBatchItemWriter<>();

        // configura el datasource
        writer.setDataSource(ds);

        // SQL de inserción con parámetros nombrados
        writer.setSql("""
            INSERT INTO annual_statements
            (account_id, year, total_credits, total_debits, ending_balance, audit_flag, audit_note)
            VALUES
            (:accountId, :year, :totalCredits, :totalDebits, :endingBalance, :auditFlag, :auditNote)
        """);

        // mapea propiedades del objeto a parámetros SQL mediante BeanPropertySqlParameterSource
        writer.setItemSqlParameterSourceProvider(
                org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource::new
        );

        return writer;
    }
}

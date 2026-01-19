package com.duoc.Backen3.tasklets;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

// tasklet encargado de generar el resumen diario de transacciones
public class DailySummaryTasklet implements Tasklet {

    // template jdbc para ejecutar consultas a la base de datos
    private final NamedParameterJdbcTemplate jdbc;

    // ruta del archivo de salida del resumen diario
    private final String outputPath;

    // constructor que recibe el template jdbc y la ruta del archivo
    public DailySummaryTasklet(
        NamedParameterJdbcTemplate jdbc,
        String outputPath
    ) {
        this.jdbc = jdbc;
        this.outputPath = outputPath;
    }

    // metodo principal que ejecuta el resumen diario
    @Override
    public RepeatStatus execute(
        StepContribution contribution,
        ChunkContext chunkContext
    ) throws Exception {

        // fecha actual del proceso
        LocalDate today = LocalDate.now();

        // obtiene el total de transacciones procesadas
        Integer total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM daily_transactions",
            Map.of(),
            Integer.class
        );

        // obtiene el total de transacciones marcadas como anomalias
        Integer anomalies = jdbc.queryForObject(
            "SELECT COUNT(*) FROM daily_transactions WHERE anomaly = true",
            Map.of(),
            Integer.class
        );

        // obtiene la suma total de montos procesados
        BigDecimal sumAmount = jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount),0) FROM daily_transactions",
            Map.of(),
            BigDecimal.class
        );

        // inserta el resumen diario en la base de datos
        jdbc.update("""
        INSERT INTO daily_summary
        (process_date, total_count, anomaly_count, total_amount)
        VALUES
        (:d, :t, :a, :s)
        """,
            Map.of(
                "d", today,
                "t", total,
                "a", anomalies,
                "s", sumAmount
            )
        );

        // crea el archivo de salida del resumen diario
        File out = new File(outputPath);
        out.getParentFile().mkdirs();

        // escritura del archivo de resumen diario
        try (FileWriter fw = new FileWriter(out, false)) {

        fw.write("daily summary - " + today + System.lineSeparator());
        fw.write("total transactions: " + total + System.lineSeparator());
        fw.write("anomalies: " + anomalies + System.lineSeparator());
        fw.write("total amount: " + sumAmount + System.lineSeparator());
        }

        // indica fin exitoso del tasklet
        return RepeatStatus.FINISHED;
    }
}

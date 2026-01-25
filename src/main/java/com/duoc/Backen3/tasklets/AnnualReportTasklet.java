package com.duoc.Backen3.tasklets;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

// tasklet encargado de generar el reporte anual de auditoria
public class AnnualReportTasklet implements Tasklet {

    // template jdbc para ejecutar consultas con parametros nombrados
    private final NamedParameterJdbcTemplate jdbc;

    // ruta de salida del archivo de reporte
    private final String outputPath;

    // constructor que recibe el template jdbc y la ruta del archivo
    public AnnualReportTasklet(
        NamedParameterJdbcTemplate jdbc,
        String outputPath
    ) {
        this.jdbc = jdbc;
        this.outputPath = outputPath;
    }

    // metodo principal que ejecuta la generacion del reporte
    @Override
    public RepeatStatus execute(
        StepContribution contribution,
        ChunkContext chunkContext
    ) throws Exception {

        // consulta de registros marcados para auditoria
        List<Map<String, Object>> audits =
            jdbc.queryForList("""
            SELECT account_id, year, audit_note
            FROM annual_statements
            WHERE audit_flag = 'true'
            ORDER BY year DESC, account_id
            """, Map.of());

        // crea el archivo de salida
        File out = new File(outputPath);
        out.getParentFile().mkdirs();

        // escritura del reporte en archivo de texto
        try (FileWriter fw = new FileWriter(out, false)) {

        // encabezado del reporte
        fw.write("annual audit report" + System.lineSeparator());
        fw.write("total flagged records: " + audits.size() + System.lineSeparator());
        fw.write(System.lineSeparator());

        // detalle de cada registro auditado
        for (Map<String, Object> row : audits) {
            fw.write(
                "account_id=" + row.get("account_id")
                    + " year=" + row.get("year")
                    + " note=" + row.get("audit_note")
                    + System.lineSeparator()
            );
        }
        }

        // indica fin exitoso del tasklet
        return RepeatStatus.FINISHED;
    }
}

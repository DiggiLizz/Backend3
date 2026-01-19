package com.duoc.Backen3.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// declara los getter y setter de manera automatica
@Getter @Setter
@Component
@ConfigurationProperties(prefix = "finabc")
public class FilePathsProperties {

    // crea e inicia las rutas de archivos
    private Files files = new Files();

    // crea e inicia el contenedor de salida
    private Output output = new Output();

    @Getter @Setter
    public static class Files {
        // para transacciones diarias
        private String dailyTransactions;

        // para cuentas
        private String accounts;

        // para estados anuales
        private String annualStatements;
    }

    // salida generada por jobs
    @Getter @Setter
    public static class Output {
        // resumen diario
        private String dailySummary;

        // reporte anual
        private String annualReport;
    }
}
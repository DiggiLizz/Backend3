
package com.duoc.Backen3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "finabc")
public class FilePathsProperties {

    private Files files = new Files();
    private Output output = new Output();

    public Files getFiles() {
        return files;
    }

    public void setFiles(Files files) {
        this.files = files;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    // ---------- Entradas ----------
    public static class Files {
        private String dailyTransactions;
        private String accounts;
        private String annualStatements;

        public String getDailyTransactions() {
            return dailyTransactions;
        }

        public void setDailyTransactions(String dailyTransactions) {
            this.dailyTransactions = dailyTransactions;
        }

        public String getAccounts() {
            return accounts;
        }

        public void setAccounts(String accounts) {
            this.accounts = accounts;
        }

        public String getAnnualStatements() {
            return annualStatements;
        }

        public void setAnnualStatements(String annualStatements) {
            this.annualStatements = annualStatements;
        }
    }

    // ---------- Salidas ----------
    public static class Output {
        private String dailySummary;
        private String annualReport;

        public String getDailySummary() {
            return dailySummary;
        }

        public void setDailySummary(String dailySummary) {
            this.dailySummary = dailySummary;
        }

        public String getAnnualReport() {
            return annualReport;
        }

        public void setAnnualReport(String annualReport) {
            this.annualReport = annualReport;
        }
    }
}

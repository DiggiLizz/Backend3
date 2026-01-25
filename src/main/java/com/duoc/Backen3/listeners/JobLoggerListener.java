package com.duoc.Backen3.listeners;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class JobLoggerListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        System.out.println("===============================================");
        System.out.println("INICIANDO JOB: " + jobExecution.getJobInstance().getJobName());
        System.out.println("===============================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        System.out.println("===============================================");
        System.out.println("FINALIZADO JOB: " + jobExecution.getJobInstance().getJobName());
        System.out.println("Estado Final: " + jobExecution.getStatus());
        System.out.println("===============================================");
    }
}
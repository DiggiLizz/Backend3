package com.duoc.Backen3.processors;

import java.math.BigDecimal;

import org.springframework.batch.item.ItemProcessor;

import com.duoc.Backen3.domain.DailyTransaction;

public class DailyTransactionProcessor implements ItemProcessor<DailyTransaction, DailyTransaction> {

    @Override
    public DailyTransaction process(DailyTransaction item) {
        // Regla 1: Validar montos negativos (Seguridad Financiera)
        if (item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            item.setAnomaly("YES");
            item.setAnomalyReason("NEGATIVE_AMOUNT");
        } 
        // Regla 2: Validar canales sospechosos (Ciberseguridad)
        else if (item.getChannel() == null || item.getChannel().isEmpty()) {
            item.setAnomaly("YES");
            item.setAnomalyReason("MISSING_CHANNEL");
        } 
        // Si todo está bien
        else {
            item.setAnomaly("NO");
            item.setAnomalyReason("NONE");
        }

        return item;
    }
}
package com.duoc.Backen3.processors;

import com.duoc.Backen3.domain.AnnualStatement;

import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;

// processor encargado de evaluar reglas de auditoria en estados anuales
public class AnnualStatementProcessor
        implements ItemProcessor<AnnualStatement, AnnualStatement> {

    // metodo principal que valida y marca estados para auditoria
    @Override
    public AnnualStatement process(AnnualStatement st) {

        // indicador de auditoria
        boolean audit = false;

        // nota asociada a la auditoria
        String note = null;

        // regla 1: saldo final negativo
        if (st.getEndingBalance().compareTo(BigDecimal.ZERO) < 0) {
        audit = true;
        note = "negative_ending_balance";
        }

        // regla 2: debitos excesivamente altos en relacion a los creditos
        if (st.getTotalDebits()
            .compareTo(st.getTotalCredits().multiply(new BigDecimal("3"))) > 0) {

        audit = true;

        // concatena observaciones si ya existe una nota previa
        note = (note == null)
            ? "debits_too_high"
            : note + ";debits_too_high";
        }

        // asigna resultados de auditoria al estado anual
        st.setAuditFlag(audit);
        st.setAuditNote(note);

        // retorna el objeto procesado
        return st;
    }
}


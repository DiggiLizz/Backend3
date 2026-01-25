package com.duoc.Backen3.processors;

import java.math.BigDecimal;

import org.springframework.batch.item.ItemProcessor;

import com.duoc.Backen3.domain.AnnualStatement;

public class AnnualStatementProcessor 
        implements ItemProcessor<AnnualStatement, AnnualStatement> {

    @Override
    public AnnualStatement process(AnnualStatement st) {
        boolean audit = false;
        StringBuilder note = new StringBuilder();

        // Regla 1: Saldo final negativo
        if (st.getEndingBalance().compareTo(BigDecimal.ZERO) < 0) {
            audit = true;
            note.append("negative_ending_balance;");
        }

        // Regla 2: Débitos excesivos (ejemplo: débitos > créditos * 2)
        if (st.getTotalDebits().compareTo(st.getTotalCredits().multiply(new BigDecimal("2"))) > 0) {
            audit = true;
            note.append("excessive_debits;");
        }

        // Asignación compatible con Oracle 
        st.setAuditFlag(audit ? "Y" : "N");
        st.setAuditNote(audit ? note.toString() : null);

        return st;
    }
}
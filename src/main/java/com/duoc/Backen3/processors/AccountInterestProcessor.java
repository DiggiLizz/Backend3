package com.duoc.Backen3.processors;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.batch.item.ItemProcessor;

import com.duoc.Backen3.domain.Account;
import com.duoc.Backen3.domain.InterestResult;

public class AccountInterestProcessor
        implements ItemProcessor<Account, InterestResult> {

    private final String quarter;

    public AccountInterestProcessor(String quarter) {
        this.quarter = quarter;
    }

    @Override
    public InterestResult process(Account acc) {

        // 1. Validación (Criterio 3 de la rúbrica: Manejo de datos)
        if (acc.getBalance() == null || acc.getAnnualRate() == null) {
            throw new IllegalArgumentException("balance or annual_rate is null");
        }

        // 2. Cálculo de tasa trimestral
        BigDecimal rateQuarter = acc.getAnnualRate()
            .divide(new BigDecimal("4"), 10, RoundingMode.HALF_UP);

        // 3. Cálculo del interés
        BigDecimal interest = acc.getBalance()
            .multiply(rateQuarter)
            .setScale(2, RoundingMode.HALF_UP);

        // 4. Cálculo del nuevo saldo
        BigDecimal newBalance = acc.getBalance().add(interest);

        // 5. Construcción MANUAL (Sin Builder) para evitar errores del IDE
        InterestResult result = new InterestResult();
        result.setAccountId(acc.getAccountId());
        result.setQuarter(this.quarter);
        result.setInterestApplied(interest);
        result.setNewBalance(newBalance);

        return result;
    }
}
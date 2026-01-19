package com.duoc.Backen3.processors;


import com.duoc.Backen3.domain.Account;
import com.duoc.Backen3.domain.InterestResult;

import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;

// processor encargado de calcular intereses trimestrales por cuenta
public class AccountInterestProcessor
        implements ItemProcessor<Account, InterestResult> {

    // identificador del trimestre en curso
    private final String quarter;

    // constructor que recibe el trimestre a procesar
    public AccountInterestProcessor(String quarter) {
        this.quarter = quarter;
    }

    // metodo principal que transforma una cuenta en un resultado de interes
    @Override
    public InterestResult process(Account acc) {

        // validacion de datos obligatorios
        if (acc.getBalance() == null || acc.getAnnualRate() == null) {
        throw new IllegalArgumentException("balance or annual_rate is null");
        }

        // calculo de la tasa trimestral a partir de la tasa anual
        BigDecimal rateQuarter = acc.getAnnualRate()
            .divide(new BigDecimal("4"), 10, RoundingMode.HALF_UP);

        // calculo del interes trimestral
        BigDecimal interest = acc.getBalance()
            .multiply(rateQuarter)
            .setScale(2, RoundingMode.HALF_UP);

        // calculo del nuevo saldo segun tipo de cuenta
        BigDecimal newBalance;

        // si es prestamo el interes incrementa la deuda
        if ("LOAN".equalsIgnoreCase(acc.getAccountType())) {
        newBalance = acc.getBalance().add(interest);
        }
        // si es ahorro el interes incrementa el saldo
        else {
        newBalance = acc.getBalance().add(interest);
        }

        // construccion del objeto resultado a persistir
        return InterestResult.builder()
            .accountId(acc.getAccountId())
            .quarter(quarter)
            .interestApplied(interest)
            .newBalance(newBalance)
            .build();
    }
}
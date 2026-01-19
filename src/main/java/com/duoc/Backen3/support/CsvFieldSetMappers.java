package com.duoc.Backen3.support;

import com.duoc.Backen3.domain.Account;
import com.duoc.Backen3.domain.AnnualStatement;
import com.duoc.Backen3.domain.DailyTransaction;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// clase utilitaria que define mapeadores para archivos csv
public class CsvFieldSetMappers {

    // mapeador para convertir una linea csv en una transaccion diaria
    public static FieldSetMapper<DailyTransaction> dailyTxMapper() {
        return (FieldSet fs) -> DailyTransaction.builder()
            // id de la transaccion
            .txId(fs.readString("tx_id"))
            // identificador de la cuenta
            .accountId(fs.readString("account_id"))
            // fecha y hora de la transaccion
            .txTimestamp(LocalDateTime.parse(fs.readString("tx_timestamp")))
            // monto de la transaccion
            .amount(new BigDecimal(fs.readString("amount")))
            // canal de origen
            .channel(fs.readString("channel"))
            .build();
    }

    // mapeador para convertir una linea csv en una cuenta
    public static FieldSetMapper<Account> accountMapper() {
        return (FieldSet fs) -> Account.builder()
            // identificador de la cuenta
            .accountId(fs.readString("account_id"))
            // tipo de cuenta ahorro o prestamo
            .accountType(fs.readString("account_type"))
            // saldo actual de la cuenta
            .balance(new BigDecimal(fs.readString("balance")))
            // tasa anual asociada a la cuenta
            .annualRate(new BigDecimal(fs.readString("annual_rate")))
            .build();
    }

    // mapeador para convertir una linea csv en un estado financiero anual
    public static FieldSetMapper<AnnualStatement> annualMapper() {
        return (FieldSet fs) -> AnnualStatement.builder()
            // identificador de la cuenta
            .accountId(fs.readString("account_id"))
            // anio del estado financiero
            .year(fs.readInt("year"))
            // total de creditos del periodo
            .totalCredits(new BigDecimal(fs.readString("total_credits")))
            // total de debitos del periodo
            .totalDebits(new BigDecimal(fs.readString("total_debits")))
            // saldo final del periodo
            .endingBalance(new BigDecimal(fs.readString("ending_balance")))
            .build();
  }
}

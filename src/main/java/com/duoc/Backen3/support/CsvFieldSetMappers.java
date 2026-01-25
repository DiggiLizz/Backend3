package com.duoc.Backen3.support;

import com.duoc.Backen3.domain.Account;
import com.duoc.Backen3.domain.AnnualStatement;
import com.duoc.Backen3.domain.DailyTransaction;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CsvFieldSetMappers {

    public static FieldSetMapper<DailyTransaction> dailyTxMapper() {
        return (FieldSet fs) -> {
            DailyTransaction tx = new DailyTransaction();
            tx.setTxId(fs.readString("tx_id"));
            tx.setAccountId(fs.readString("account_id"));
            tx.setTxTimestamp(LocalDateTime.parse(fs.readString("tx_timestamp")));
            tx.setAmount(new BigDecimal(fs.readString("amount")));
            tx.setChannel(fs.readString("channel"));
            return tx;
        };
    }

    public static FieldSetMapper<Account> accountMapper() {
        return (FieldSet fs) -> {
            Account acc = new Account();
            acc.setAccountId(fs.readString("account_id"));
            acc.setAccountType(fs.readString("account_type"));
            acc.setBalance(new BigDecimal(fs.readString("balance")));
            acc.setAnnualRate(new BigDecimal(fs.readString("annual_rate")));
            return acc;
        };
    }

    public static FieldSetMapper<AnnualStatement> annualMapper() {
        return (FieldSet fs) -> {
            AnnualStatement as = new AnnualStatement();
            as.setAccountId(fs.readString("account_id"));
            as.setYear(fs.readInt("year"));
            as.setTotalCredits(new BigDecimal(fs.readString("total_credits")));
            as.setTotalDebits(new BigDecimal(fs.readString("total_debits")));
            as.setEndingBalance(new BigDecimal(fs.readString("ending_balance")));
            return as;
        };
    }
}
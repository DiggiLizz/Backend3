package com.duoc.Backen3.domain;

import java.math.BigDecimal;

public class Account {
    private String accountId;
    private String accountType;
    private BigDecimal balance;
    private BigDecimal annualRate;

    public Account() {}

    // Getters
    public String getAccountId() { return accountId; }
    public String getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getAnnualRate() { return annualRate; }

    // Setters
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setAnnualRate(BigDecimal annualRate) { this.annualRate = annualRate; }

    @Override
    public String toString() {
        return "Account(id=" + accountId + ", type=" + accountType + ")";
    }
}
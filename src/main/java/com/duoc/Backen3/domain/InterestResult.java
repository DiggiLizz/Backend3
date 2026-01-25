package com.duoc.Backen3.domain;

import java.math.BigDecimal;

public class InterestResult {
    private Long id;
    private String accountId;
    private String quarter;
    private BigDecimal interestApplied;
    private BigDecimal newBalance;

    public InterestResult() {}

    // Getters
    public Long getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getQuarter() { return quarter; }
    public BigDecimal getInterestApplied() { return interestApplied; }
    public BigDecimal getNewBalance() { return newBalance; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public void setQuarter(String quarter) { this.quarter = quarter; }
    public void setInterestApplied(BigDecimal interestApplied) { this.interestApplied = interestApplied; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }

    @Override
    public String toString() {
        return "InterestResult(accountId=" + accountId + ", quarter=" + quarter + ")";
    }
}
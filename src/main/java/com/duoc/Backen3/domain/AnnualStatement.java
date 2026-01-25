package com.duoc.Backen3.domain;

import java.math.BigDecimal;

public class AnnualStatement {
    private String accountId;
    private int year;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private BigDecimal endingBalance;
    private String auditFlag;
    private String auditNote;

    // Constructor vacío para Spring Batch
    public AnnualStatement() {}

    // GETTERS 
    public BigDecimal getEndingBalance() { return endingBalance; }
    public BigDecimal getTotalDebits() { return totalDebits; }
    public BigDecimal getTotalCredits() { return totalCredits; }
    public String getAccountId() { return accountId; }
    public int getYear() { return year; }
    public String getAuditFlag() { return auditFlag; }
    public String getAuditNote() { return auditNote; }

    // SETTERS
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public void setYear(int year) { this.year = year; }
    public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }
    public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; }
    public void setEndingBalance(BigDecimal endingBalance) { this.endingBalance = endingBalance; }
    public void setAuditFlag(String auditFlag) { this.auditFlag = auditFlag; }
    public void setAuditNote(String auditNote) { this.auditNote = auditNote; }
}
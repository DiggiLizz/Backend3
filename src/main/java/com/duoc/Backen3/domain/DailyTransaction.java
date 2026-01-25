package com.duoc.Backen3.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DailyTransaction {
    private String txId;
    private String accountId;
    private LocalDateTime txTimestamp;
    private BigDecimal amount;
    private String channel;
    private String anomaly; 
    private String anomalyReason;

    public DailyTransaction() {}

    // Getters
    public String getTxId() { return txId; }
    public String getAccountId() { return accountId; }
    public LocalDateTime getTxTimestamp() { return txTimestamp; }
    public BigDecimal getAmount() { return amount; }
    public String getChannel() { return channel; }
    public String getAnomaly() { return anomaly; }
    public String getAnomalyReason() { return anomalyReason; }

    // Setters
    public void setTxId(String txId) { this.txId = txId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public void setTxTimestamp(LocalDateTime txTimestamp) { this.txTimestamp = txTimestamp; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setChannel(String channel) { this.channel = channel; }
    public void setAnomaly(String anomaly) { this.anomaly = anomaly; }
    public void setAnomalyReason(String anomalyReason) { this.anomalyReason = anomalyReason; }
}
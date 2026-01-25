package com.duoc.Backen3.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailySummary {
    private LocalDate processDate;
    private int totalCount;
    private int anomalyCount;
    private BigDecimal totalAmount;

    public DailySummary() {}

    // Getters
    public LocalDate getProcessDate() { return processDate; }
    public int getTotalCount() { return totalCount; }
    public int getAnomalyCount() { return anomalyCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }

    // Setters
    public void setProcessDate(LocalDate processDate) { this.processDate = processDate; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public void setAnomalyCount(int anomalyCount) { this.anomalyCount = anomalyCount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
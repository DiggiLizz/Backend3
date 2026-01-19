package com.duoc.Backen3.domain;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailySummary {
  private LocalDate processDate;
  private int totalCount;
  private int anomalyCount;
  private BigDecimal totalAmount;
}


package com.duoc.Backen3.domain;


import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnualStatement {
  private String accountId;
  private int year;
  private BigDecimal totalCredits;
  private BigDecimal totalDebits;
  private BigDecimal endingBalance;

  private boolean auditFlag;
  private String auditNote;
}


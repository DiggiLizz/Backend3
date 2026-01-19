package com.duoc.Backen3.domain;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {
  private String accountId;
  private String accountType; // SAVINGS / LOAN
  private BigDecimal balance;
  private BigDecimal annualRate;
}


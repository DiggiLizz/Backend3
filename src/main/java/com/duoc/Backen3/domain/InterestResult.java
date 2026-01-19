package com.duoc.Backen3.domain;


import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterestResult {
    private String accountId;
    private String quarter;
    private BigDecimal interestApplied;
    private BigDecimal newBalance;
}


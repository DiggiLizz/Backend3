package com.duoc.Backen3.domain;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyTransaction {
    private String txId;
    private String accountId;
    private LocalDateTime txTimestamp;
    private BigDecimal amount;
    private String channel;

    private boolean anomaly;
    private String anomalyReason;
}

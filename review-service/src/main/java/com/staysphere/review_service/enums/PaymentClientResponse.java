package com.staysphere.review_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentClientResponse {

    private Long id;
    private Long bookingId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;
    private String paymentProvider;
    private String providerTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
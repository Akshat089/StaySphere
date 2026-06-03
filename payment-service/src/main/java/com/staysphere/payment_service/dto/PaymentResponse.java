package com.staysphere.payment_service.dto;
import com.staysphere.payment_service.enums.PaymentProvider;
import com.staysphere.payment_service.enums.PaymentStatus;
import lombok.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus paymentStatus;
    private PaymentProvider paymentProvider;
    private String providerTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.staysphere.payment_service.dto;

import com.staysphere.payment_service.enums.PaymentProvider;
import lombok.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CreatePaymentRequest {
    @NotNull(message = "Booking Id is required")
    private Long bookingId;

    @NotNull(message = "UserId is required")
    private Long userId;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "payment provider is required")
    private PaymentProvider paymentProvider;
}

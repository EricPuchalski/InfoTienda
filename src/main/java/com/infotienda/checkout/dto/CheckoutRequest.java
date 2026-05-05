package com.infotienda.checkout.dto;

import com.infotienda.order.model.DeliveryMethod;
import com.infotienda.order.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotNull(message = "deliveryMethod is required")
    private DeliveryMethod deliveryMethod;

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    @Valid
    private CheckoutAddressRequest shippingAddress;

    @Size(max = 150, message = "pickupStoreName must be at most 150 characters")
    private String pickupStoreName;

    @Size(max = 150, message = "receiverName must be at most 150 characters")
    private String receiverName;
}

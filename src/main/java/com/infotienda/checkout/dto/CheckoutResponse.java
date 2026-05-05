package com.infotienda.checkout.dto;

import com.infotienda.order.model.DeliveryMethod;
import com.infotienda.order.model.OrderStatus;
import com.infotienda.order.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {

    private Long orderId;
    private DeliveryMethod deliveryMethod;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private String pickupStoreName;
    private String receiverName;
    private String mercadoPagoPreferenceId;
    private String mercadoPagoInitPoint;
    private String mercadoPagoSandboxInitPoint;
    private CheckoutAddressResponse shippingAddress;
    private List<CheckoutItemResponse> items;
}

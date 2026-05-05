package com.infotienda.cart.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subTotal;
}

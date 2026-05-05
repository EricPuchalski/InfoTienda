package com.infotienda.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutAddressResponse {

    private Long id;
    private String street;
    private String number;
    private String apartment;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String reference;
}

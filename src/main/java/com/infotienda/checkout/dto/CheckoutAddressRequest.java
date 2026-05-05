package com.infotienda.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutAddressRequest {

    @NotBlank(message = "street is required")
    @Size(max = 255, message = "street must be at most 255 characters")
    private String street;

    @Size(max = 50, message = "number must be at most 50 characters")
    private String number;

    @Size(max = 100, message = "apartment must be at most 100 characters")
    private String apartment;

    @NotBlank(message = "city is required")
    @Size(max = 100, message = "city must be at most 100 characters")
    private String city;

    @NotBlank(message = "state is required")
    @Size(max = 100, message = "state must be at most 100 characters")
    private String state;

    @NotBlank(message = "postalCode is required")
    @Size(max = 20, message = "postalCode must be at most 20 characters")
    private String postalCode;

    @NotBlank(message = "country is required")
    @Size(max = 100, message = "country must be at most 100 characters")
    private String country;

    @Size(max = 255, message = "reference must be at most 255 characters")
    private String reference;
}

package com.infotienda.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MercadoPagoPreferenceResult {

    private final String preferenceId;
    private final String initPoint;
    private final String sandboxInitPoint;
    private final String externalReference;
}

package com.infotienda.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mercadopago")
public class MercadoPagoProperties {

    private String accessToken;
    private String successUrl;
    private String pendingUrl;
    private String failureUrl;
    private String notificationUrl;
    private String webhookSecret;
    private String currencyId ;
    private String statementDescriptor;
}

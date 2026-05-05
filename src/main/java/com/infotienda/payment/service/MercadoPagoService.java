package com.infotienda.payment.service;

import com.infotienda.order.model.Order;
import com.infotienda.payment.dto.MercadoPagoPreferenceResult;
import java.util.Map;

public interface MercadoPagoService {

    MercadoPagoPreferenceResult createCheckoutPreference(Order order);

    void processWebhook(String type, String dataId, Map<String, Object> payload, String xSignature, String xRequestId);
}

package com.infotienda.payment.service.impl;

import com.infotienda.core.exception.PaymentIntegrationException;
import com.infotienda.order.model.Order;
import com.infotienda.order.model.OrderItem;
import com.infotienda.order.model.OrderStatus;
import com.infotienda.order.repository.OrderRepository;
import com.infotienda.payment.config.MercadoPagoProperties;
import com.infotienda.payment.dto.MercadoPagoPreferenceResult;
import com.infotienda.payment.service.MercadoPagoService;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPResponse;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import java.net.URI;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private static final String PAYMENT_TOPIC = "payment";

    private final MercadoPagoProperties mercadoPagoProperties;
    private final OrderRepository orderRepository;

    @Override
    public MercadoPagoPreferenceResult createCheckoutPreference(Order order) {
        validateConfiguration();

        String externalReference = buildExternalReference(order);
        String successUrl = normalize(mercadoPagoProperties.getSuccessUrl());
        String pendingUrl = normalize(mercadoPagoProperties.getPendingUrl());
        String failureUrl = normalize(mercadoPagoProperties.getFailureUrl());
        String notificationUrl = normalize(mercadoPagoProperties.getNotificationUrl());
        String autoReturn = resolveAutoReturn(successUrl);
        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(buildItems(order.getOrderItems()))
                .payer(buildPayer(order))
                .backUrls(buildBackUrls(successUrl, pendingUrl, failureUrl))
                .autoReturn(autoReturn)
                .externalReference(externalReference)
                .notificationUrl(notificationUrl)
                .statementDescriptor(mercadoPagoProperties.getStatementDescriptor())
                .build();

        log.info(
                "Creating Mercado Pago preference for orderId={} successUrl={} pendingUrl={} failureUrl={} notificationUrl={} autoReturn={}",
                order.getId(),
                successUrl,
                pendingUrl,
                failureUrl,
                notificationUrl,
                autoReturn
        );

        try {
            Preference preference = new PreferenceClient().create(preferenceRequest, buildRequestOptions());
            return MercadoPagoPreferenceResult.builder()
                    .preferenceId(preference.getId())
                    .initPoint(preference.getInitPoint())
                    .sandboxInitPoint(preference.getSandboxInitPoint())
                    .externalReference(externalReference)
                    .build();
        } catch (MPApiException ex) {
            throw new PaymentIntegrationException(buildApiErrorMessage("create Mercado Pago preference", ex), ex);
        } catch (MPException ex) {
            throw new PaymentIntegrationException("Could not create Mercado Pago preference: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new PaymentIntegrationException(
                    "Could not create Mercado Pago preference: unexpected error " + ex.getClass().getSimpleName()
                            + " - " + ex.getMessage(),
                    ex
            );
        }
    }

    @Override
    @Transactional
    public void processWebhook(String type, String dataId, Map<String, Object> payload, String xSignature, String xRequestId) {
        String notificationType = firstNonBlank(type, extractTypeFromPayload(payload));
        if (!PAYMENT_TOPIC.equals(notificationType)) {
            return;
        }

        String paymentIdValue = firstNonBlank(dataId, extractDataIdFromPayload(payload));
        if (paymentIdValue == null) {
            throw new PaymentIntegrationException("Mercado Pago webhook arrived without payment id");
        }

        verifySignatureIfConfigured(paymentIdValue, xSignature, xRequestId);

        Payment payment = fetchPayment(Long.valueOf(paymentIdValue));
        if (payment.getExternalReference() == null || payment.getExternalReference().isBlank()) {
            return;
        }

        Order order = orderRepository.findByExternalReference(payment.getExternalReference())
                .orElseThrow(() -> new PaymentIntegrationException(
                        "Order not found for external reference: " + payment.getExternalReference()));

        order.setMercadoPagoPaymentId(payment.getId());
        order.setMercadoPagoStatus(payment.getStatus());
        order.setMercadoPagoStatusDetail(payment.getStatusDetail());
        order.setStatus(mapOrderStatus(payment.getStatus()));
        orderRepository.save(order);
    }

    private void validateConfiguration() {
        if (isBlank(mercadoPagoProperties.getAccessToken())) {
            throw new PaymentIntegrationException("MERCADO_PAGO_ACCESS_TOKEN is not configured");
        }
        if (isBlank(mercadoPagoProperties.getSuccessUrl())
                || isBlank(mercadoPagoProperties.getPendingUrl())
                || isBlank(mercadoPagoProperties.getFailureUrl())) {
            throw new PaymentIntegrationException("Mercado Pago back URLs are not fully configured");
        }
    }

    private List<PreferenceItemRequest> buildItems(List<OrderItem> orderItems) {
        return new ArrayList<>(orderItems.stream()
                .map(this::buildItem)
                .toList());
    }

    private PreferenceItemRequest buildItem(OrderItem orderItem) {
        return PreferenceItemRequest.builder()
                .id(String.valueOf(orderItem.getProduct().getId()))
                .title(orderItem.getProduct().getName())
                .description(orderItem.getProduct().getDescription())
                .pictureUrl(orderItem.getProduct().getImageUrl())
                .categoryId(orderItem.getProduct().getCategory().getName())
                .currencyId(mercadoPagoProperties.getCurrencyId())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .build();
    }

    private PreferencePayerRequest buildPayer(Order order) {
        return PreferencePayerRequest.builder()
                .name(order.getUser().getFirstName())
                .surname(order.getUser().getLastName())
                .email(order.getUser().getEmail())
                .build();
    }

    private PreferenceBackUrlsRequest buildBackUrls(String successUrl, String pendingUrl, String failureUrl) {
        return PreferenceBackUrlsRequest.builder()
                .success(successUrl)
                .pending(pendingUrl)
                .failure(failureUrl)
                .build();
    }

    private MPRequestOptions buildRequestOptions() {
        Map<String, String> customHeaders = new java.util.HashMap<>();
        customHeaders.put("X-Idempotency-Key", UUID.randomUUID().toString());
        return MPRequestOptions.builder()
                .accessToken(mercadoPagoProperties.getAccessToken())
                .customHeaders(customHeaders)
                .build();
    }

    private String buildExternalReference(Order order) {
        return "order-" + order.getId();
    }

    private Payment fetchPayment(Long paymentId) {
        try {
            return new PaymentClient().get(paymentId, buildRequestOptions());
        } catch (MPApiException ex) {
            throw new PaymentIntegrationException(
                    buildApiErrorMessage("fetch Mercado Pago payment " + paymentId, ex), ex);
        } catch (MPException ex) {
            throw new PaymentIntegrationException("Could not fetch Mercado Pago payment " + paymentId + ": " + ex.getMessage(), ex);
        }
    }

    private OrderStatus mapOrderStatus(String mercadoPagoStatus) {
        if (mercadoPagoStatus == null) {
            return OrderStatus.PENDING;
        }
        return switch (mercadoPagoStatus) {
            case "approved" -> OrderStatus.PAID;
            case "cancelled", "rejected", "refunded", "charged_back" -> OrderStatus.CANCELLED;
            default -> OrderStatus.PENDING;
        };
    }

    private void verifySignatureIfConfigured(String dataId, String xSignature, String xRequestId) {
        if (isBlank(mercadoPagoProperties.getWebhookSecret())) {
            return;
        }

        if (isBlank(xSignature) || isBlank(xRequestId)) {
            throw new PaymentIntegrationException("Mercado Pago webhook signature headers are missing");
        }

        String timestamp = extractSignaturePart(xSignature, "ts");
        String hash = extractSignaturePart(xSignature, "v1");
        if (timestamp == null || hash == null) {
            throw new PaymentIntegrationException("Mercado Pago webhook signature is malformed");
        }

        String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + timestamp + ";";
        String expected = sign(manifest, mercadoPagoProperties.getWebhookSecret());
        if (!Objects.equals(expected, hash)) {
            throw new PaymentIntegrationException("Mercado Pago webhook signature is invalid");
        }
    }

    private String sign(String manifest, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new PaymentIntegrationException("Could not verify Mercado Pago webhook signature", ex);
        }
    }

    private String extractSignaturePart(String headerValue, String key) {
        String[] pairs = headerValue.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2 && key.equalsIgnoreCase(keyValue[0].trim())) {
                return keyValue[1].trim();
            }
        }
        return null;
    }

    private String extractTypeFromPayload(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get("type");
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractDataIdFromPayload(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }

        Object data = payload.get("data");
        if (data instanceof Map<?, ?> map) {
            Object id = ((Map<String, Object>) map).get("id");
            return id == null ? null : id.toString();
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : (!isBlank(second) ? second : null);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String resolveAutoReturn(String successUrl) {
        if (isBlank(successUrl) || isLocalUrl(successUrl)) {
            return null;
        }
        return "approved";
    }

    private boolean isLocalUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host == null
                    || "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "0.0.0.0".equals(host);
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    private String buildApiErrorMessage(String action, MPApiException ex) {
        MPResponse response = ex.getApiResponse();
        if (response == null) {
            return "Could not " + action + ": " + ex.getMessage();
        }

        String content = response.getContent();
        if (content == null || content.isBlank()) {
            return "Could not " + action + ": Mercado Pago API returned status " + response.getStatusCode();
        }

        return "Could not " + action + ": Mercado Pago API returned status "
                + response.getStatusCode() + " with body " + content;
    }
}

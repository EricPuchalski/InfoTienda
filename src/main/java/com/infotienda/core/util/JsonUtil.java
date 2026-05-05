package com.infotienda.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotienda.catalog.dto.ProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonUtil {

    private final ObjectMapper objectMapper;

    public ProductRequest parseProductRequest(String productJson) {
        try {
            return objectMapper.readValue(productJson, ProductRequest.class);
        } catch (JsonProcessingException ex) {
            throw new HttpMessageNotReadableException("Invalid 'product' JSON payload", ex, null);
        }
    }
}

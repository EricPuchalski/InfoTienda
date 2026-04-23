package com.infotienda.checkout.service;

import com.infotienda.checkout.dto.CheckoutRequest;
import com.infotienda.checkout.dto.CheckoutResponse;

public interface CheckoutService {

    CheckoutResponse createCheckout(String userEmail, CheckoutRequest request);
}

package com.infotienda.cart.service;

import com.infotienda.cart.dto.AddToCartRequest;
import com.infotienda.cart.dto.CartResponse;
import com.infotienda.cart.dto.UpdateCartItemRequest;

public interface CartService {
    CartResponse getCartForUser(String userEmail);
    CartResponse addItemToCart(String userEmail, AddToCartRequest request);
    CartResponse updateItemQuantity(String userEmail, Long cartItemId, UpdateCartItemRequest request);
    CartResponse removeItemFromCart(String userEmail, Long cartItemId);
    void clearCart(String userEmail);
}

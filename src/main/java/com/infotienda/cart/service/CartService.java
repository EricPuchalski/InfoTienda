package com.infotienda.cart.service;

import com.infotienda.cart.dto.AddToCartRequest;
import com.infotienda.cart.dto.CartResponse;
import com.infotienda.cart.dto.UpdateCartItemRequest;
import java.util.List;

public interface CartService {
    CartResponse getCartForUser(String userEmail);
    CartResponse getCartForGuestSession(String guestSessionId);
    CartResponse addItemToCart(String userEmail, AddToCartRequest request);
    CartResponse addItemToGuestCart(String guestSessionId, AddToCartRequest request);
    CartResponse mergeGuestCart(String userEmail, List<AddToCartRequest> guestItems);
    void mergeGuestSessionCartIntoUser(String guestSessionId, String userEmail);
    CartResponse updateItemQuantity(String userEmail, Long cartItemId, UpdateCartItemRequest request);
    CartResponse updateGuestItemQuantity(String guestSessionId, Long cartItemId, UpdateCartItemRequest request);
    CartResponse removeItemFromCart(String userEmail, Long cartItemId);
    CartResponse removeItemFromGuestCart(String guestSessionId, Long cartItemId);
    void clearCart(String userEmail);
    void clearGuestCart(String guestSessionId);
}

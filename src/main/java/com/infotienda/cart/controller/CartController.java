package com.infotienda.cart.controller;

import com.infotienda.cart.dto.AddToCartRequest;
import com.infotienda.cart.dto.CartResponse;
import com.infotienda.cart.dto.UpdateCartItemRequest;
import com.infotienda.cart.service.CartService;
import com.infotienda.cart.service.GuestSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final GuestSessionService guestSessionService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (userDetails != null) {
            return ResponseEntity.ok(cartService.getCartForUser(userDetails.getUsername()));
        }

        String guestSessionId = guestSessionService.resolveOrCreateGuestSessionId(request, response);
        return ResponseEntity.ok(cartService.getCartForGuestSession(guestSessionId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @RequestBody AddToCartRequest addRequest) {
        if (userDetails != null) {
            return ResponseEntity.ok(cartService.addItemToCart(userDetails.getUsername(), addRequest));
        }

        String guestSessionId = guestSessionService.resolveOrCreateGuestSessionId(request, response);
        return ResponseEntity.ok(cartService.addItemToGuestCart(guestSessionId, addRequest));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest updateRequest) {
        if (userDetails != null) {
            return ResponseEntity.ok(cartService.updateItemQuantity(userDetails.getUsername(), cartItemId, updateRequest));
        }

        String guestSessionId = guestSessionService.resolveOrCreateGuestSessionId(request, response);
        return ResponseEntity.ok(cartService.updateGuestItemQuantity(guestSessionId, cartItemId, updateRequest));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItemFromCart(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable Long cartItemId) {
        if (userDetails != null) {
            return ResponseEntity.ok(cartService.removeItemFromCart(userDetails.getUsername(), cartItemId));
        }

        String guestSessionId = guestSessionService.resolveOrCreateGuestSessionId(request, response);
        return ResponseEntity.ok(cartService.removeItemFromGuestCart(guestSessionId, cartItemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (userDetails != null) {
            cartService.clearCart(userDetails.getUsername());
            return ResponseEntity.noContent().build();
        }

        String guestSessionId = guestSessionService.resolveOrCreateGuestSessionId(request, response);
        cartService.clearGuestCart(guestSessionId);
        return ResponseEntity.noContent().build();
    }
}

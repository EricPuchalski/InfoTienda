package com.infotienda.cart.controller;

import com.infotienda.cart.dto.AddToCartRequest;
import com.infotienda.cart.dto.CartResponse;
import com.infotienda.cart.dto.UpdateCartItemRequest;
import com.infotienda.cart.service.CartService;
import com.infotienda.core.exception.ResourceNotFoundException;
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

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        return ResponseEntity.ok(cartService.getCartForUser(userDetails.getUsername()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddToCartRequest request) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        return ResponseEntity.ok(cartService.addItemToCart(userDetails.getUsername(), request));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        return ResponseEntity.ok(cartService.updateItemQuantity(userDetails.getUsername(), cartItemId, request));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItemFromCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cartItemId) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        return ResponseEntity.ok(cartService.removeItemFromCart(userDetails.getUsername(), cartItemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        cartService.clearCart(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}

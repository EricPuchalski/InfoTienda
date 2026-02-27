package com.infotienda.cart.service.impl;

import com.infotienda.cart.dto.AddToCartRequest;
import com.infotienda.cart.dto.CartResponse;
import com.infotienda.cart.dto.UpdateCartItemRequest;
import com.infotienda.cart.mapper.CartMapper;
import com.infotienda.cart.model.Cart;
import com.infotienda.cart.model.CartItem;
import com.infotienda.cart.repository.CartItemRepository;
import com.infotienda.cart.repository.CartRepository;
import com.infotienda.cart.service.CartService;
import com.infotienda.catalog.model.Product;
import com.infotienda.catalog.repository.ProductRepository;
import com.infotienda.core.exception.ResourceNotFoundException;
import com.infotienda.security.model.User;
import com.infotienda.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartForUser(String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(String userEmail, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userEmail);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQuantity) {
                throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
            }
            existingItem.setQuantity(newQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        cart = cartRepository.save(cart);
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(String userEmail, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userEmail);

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found with id: " + cartItemId));

        if (item.getProduct().getStockQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("Not enough stock for product: " + item.getProduct().getName());
        }

        item.setQuantity(request.getQuantity());
        cart = cartRepository.save(cart);
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItemFromCart(String userEmail, Long cartItemId) {
        Cart cart = getOrCreateCart(userEmail);

        CartItem itemToRemove = cart.getItems().stream()
                .filter(ci -> ci.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found with id: " + cartItemId));

        cart.getItems().remove(itemToRemove);
        cartItemRepository.delete(itemToRemove);
        cart = cartRepository.save(cart);
        
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional
    protected Cart getOrCreateCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }
}

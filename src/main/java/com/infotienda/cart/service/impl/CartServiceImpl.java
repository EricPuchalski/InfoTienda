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
import com.infotienda.core.exception.ResourceConflictException;
import com.infotienda.core.exception.ResourceNotFoundException;
import com.infotienda.security.model.User;
import com.infotienda.security.repository.UserRepository;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional
    public CartResponse getCartForUser(String userEmail) {
        Cart cart = getOrCreateUserCart(userEmail);
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse getCartForGuestSession(String guestSessionId) {
        Cart cart = getOrCreateGuestCart(guestSessionId);
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(String userEmail, AddToCartRequest request) {
        Cart cart = getOrCreateUserCart(userEmail);
        mergeItemIntoCart(cart, request.getProductId(), request.getQuantity());
        return cartMapper.toCartResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse addItemToGuestCart(String guestSessionId, AddToCartRequest request) {
        Cart cart = getOrCreateGuestCart(guestSessionId);
        mergeItemIntoCart(cart, request.getProductId(), request.getQuantity());
        return cartMapper.toCartResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse mergeGuestCart(String userEmail, List<AddToCartRequest> guestItems) {
        Cart cart = getOrCreateUserCart(userEmail);

        if (guestItems == null || guestItems.isEmpty()) {
            return cartMapper.toCartResponse(cart);
        }

        aggregateQuantities(guestItems)
                .forEach((productId, quantity) -> mergeItemIntoCart(cart, productId, quantity));

        return cartMapper.toCartResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void mergeGuestSessionCartIntoUser(String guestSessionId, String userEmail) {
        Optional<Cart> guestCartOptional = cartRepository.findByGuestSessionId(guestSessionId);
        if (guestCartOptional.isEmpty()) {
            return;
        }

        Cart guestCart = guestCartOptional.get();
        Cart userCart = getOrCreateUserCart(userEmail);

        List<AddToCartRequest> guestItems = guestCart.getItems().stream()
                .map(item -> AddToCartRequest.builder()
                        .productId(item.getProduct().getId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        if (!guestItems.isEmpty()) {
            aggregateQuantities(guestItems)
                    .forEach((productId, quantity) -> mergeItemIntoCart(userCart, productId, quantity));
            cartRepository.save(userCart);
        }

        cartRepository.delete(guestCart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(String userEmail, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateUserCart(userEmail);
        CartItem item = findCartItemOrThrow(cart, cartItemId);

        ensureProductActiveOrThrow(item.getProduct());
        ensureStockOrThrow(item.getProduct(), request.getQuantity());

        item.setQuantity(request.getQuantity());
        cart = cartRepository.save(cart);
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateGuestItemQuantity(String guestSessionId, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateGuestCart(guestSessionId);
        CartItem item = findCartItemOrThrow(cart, cartItemId);

        ensureProductActiveOrThrow(item.getProduct());
        ensureStockOrThrow(item.getProduct(), request.getQuantity());

        item.setQuantity(request.getQuantity());
        cart = cartRepository.save(cart);
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItemFromCart(String userEmail, Long cartItemId) {
        Cart cart = getOrCreateUserCart(userEmail);
        CartItem itemToRemove = findCartItemOrThrow(cart, cartItemId);

        cart.getItems().remove(itemToRemove);
        cartItemRepository.delete(itemToRemove);
        cart = cartRepository.save(cart);

        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItemFromGuestCart(String guestSessionId, Long cartItemId) {
        Cart cart = getOrCreateGuestCart(guestSessionId);
        CartItem itemToRemove = findCartItemOrThrow(cart, cartItemId);

        cart.getItems().remove(itemToRemove);
        cartItemRepository.delete(itemToRemove);
        cart = cartRepository.save(cart);

        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(String userEmail) {
        Cart cart = getOrCreateUserCart(userEmail);
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearGuestCart(String guestSessionId) {
        Cart cart = getOrCreateGuestCart(guestSessionId);
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional
    protected Cart getOrCreateUserCart(String userEmail) {
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

    @Transactional
    protected Cart getOrCreateGuestCart(String guestSessionId) {
        return cartRepository.findByGuestSessionId(guestSessionId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .guestSessionId(guestSessionId)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private Product getActiveProductOrThrow(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        ensureProductActiveOrThrow(product);
        return product;
    }

    private void ensureProductActiveOrThrow(Product product) {
        if (!product.isActive()) {
            throw new ResourceConflictException("Product is not available: " + product.getName());
        }
    }

    private void ensureStockOrThrow(Product product, int requestedQuantity) {
        if (product.getStockQuantity() < requestedQuantity) {
            throw new ResourceConflictException("Not enough stock for product: " + product.getName());
        }
    }

    private void mergeItemIntoCart(Cart cart, Long productId, int quantity) {
        Product product = getActiveProductOrThrow(productId);

        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> updateExistingItem(existing, product, quantity),
                        () -> addNewItem(cart, product, quantity)
                );
    }

    private void updateExistingItem(CartItem existing, Product product, int quantityToAdd) {
        int newQuantity = existing.getQuantity() + quantityToAdd;
        ensureStockOrThrow(product, newQuantity);
        existing.setQuantity(newQuantity);
    }

    private void addNewItem(Cart cart, Product product, int quantity) {
        ensureStockOrThrow(product, quantity);
        cart.getItems().add(CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .build());
    }

    private Map<Long, Integer> aggregateQuantities(List<AddToCartRequest> items) {
        return items.stream()
                .collect(Collectors.toMap(
                        AddToCartRequest::getProductId,
                        AddToCartRequest::getQuantity,
                        Integer::sum,
                        LinkedHashMap::new
                ));
    }

    private CartItem findCartItemOrThrow(Cart cart, Long cartItemId) {
        return cart.getItems().stream()
                .filter(ci -> ci.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found with id: " + cartItemId));
    }

}

package com.infotienda.checkout.service.impl;

import com.infotienda.cart.model.Cart;
import com.infotienda.cart.model.CartItem;
import com.infotienda.cart.repository.CartRepository;
import com.infotienda.catalog.model.Product;
import com.infotienda.checkout.dto.CheckoutAddressRequest;
import com.infotienda.checkout.dto.CheckoutAddressResponse;
import com.infotienda.checkout.dto.CheckoutItemResponse;
import com.infotienda.checkout.dto.CheckoutRequest;
import com.infotienda.checkout.dto.CheckoutResponse;
import com.infotienda.checkout.service.CheckoutService;
import com.infotienda.core.exception.ResourceConflictException;
import com.infotienda.core.exception.ResourceNotFoundException;
import com.infotienda.order.model.DeliveryMethod;
import com.infotienda.order.model.Order;
import com.infotienda.order.model.OrderItem;
import com.infotienda.order.model.OrderStatus;
import com.infotienda.order.model.PaymentMethod;
import com.infotienda.order.repository.OrderRepository;
import com.infotienda.payment.dto.MercadoPagoPreferenceResult;
import com.infotienda.payment.service.MercadoPagoService;
import com.infotienda.security.model.Address;
import com.infotienda.security.model.User;
import com.infotienda.security.repository.AddressRepository;
import com.infotienda.security.repository.UserRepository;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final MercadoPagoService mercadoPagoService;

    @Override
    @Transactional
    public CheckoutResponse createCheckout(String userEmail, CheckoutRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceConflictException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new ResourceConflictException("Cart is empty");
        }

        validateCartItems(cart.getItems());

        validateDeliveryRequest(request);

        Address shippingAddress = null;
        if (request.getDeliveryMethod() == DeliveryMethod.SHIPPING) {
            shippingAddress = addressRepository.save(buildAddress(user, request.getShippingAddress()));
        }

        Order order = new Order();
        order.setUser(user);
        order.setDeliveryMethod(request.getDeliveryMethod());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingAddress(shippingAddress);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(calculateTotal(cart.getItems()));
        order.setPickupStoreName(normalize(request.getPickupStoreName()));
        order.setReceiverName(normalize(request.getReceiverName()));
        order.setOrderItems(buildOrderItems(order, cart.getItems()));

        Order savedOrder = orderRepository.save(order);
        MercadoPagoPreferenceResult preference = mercadoPagoService.createCheckoutPreference(savedOrder);
        savedOrder.setExternalReference(preference.getExternalReference());
        savedOrder.setMercadoPagoPreferenceId(preference.getPreferenceId());
        savedOrder.setMercadoPagoInitPoint(preference.getInitPoint());
        savedOrder.setMercadoPagoSandboxInitPoint(preference.getSandboxInitPoint());
        savedOrder = orderRepository.save(savedOrder);
        return mapToResponse(savedOrder);
    }

    private void validateDeliveryRequest(CheckoutRequest request) {
        if (request.getPaymentMethod() != PaymentMethod.MERCADO_PAGO) {
            throw new ResourceConflictException("paymentMethod is not supported");
        }

        if (request.getDeliveryMethod() == DeliveryMethod.SHIPPING) {
            if (request.getShippingAddress() == null) {
                throw new ResourceConflictException("shippingAddress is required for SHIPPING delivery");
            }
            return;
        }

        if (request.getDeliveryMethod() == DeliveryMethod.PICKUP) {
            if (isBlank(request.getPickupStoreName())) {
                throw new ResourceConflictException("pickupStoreName is required for PICKUP delivery");
            }
            if (isBlank(request.getReceiverName())) {
                throw new ResourceConflictException("receiverName is required for PICKUP delivery");
            }
        }
    }

    private void validateCartItems(List<CartItem> items) {
        for (CartItem cartItem : items) {
            Product product = cartItem.getProduct();
            if (!product.isActive()) {
                throw new ResourceConflictException("Product is not available: " + product.getName());
            }
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new ResourceConflictException("Not enough stock for product: " + product.getName());
            }
        }
    }

    private Address buildAddress(User user, CheckoutAddressRequest request) {
        Address address = new Address();
        address.setUser(user);
        address.setStreet(request.getStreet());
        address.setNumber(request.getNumber());
        address.setApartment(request.getApartment());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setReference(request.getReference());
        return address;
    }

    private List<OrderItem> buildOrderItems(Order order, List<CartItem> cartItems) {
        return new ArrayList<>(cartItems.stream()
                .map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProduct(cartItem.getProduct());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setUnitPrice(cartItem.getProduct().getPrice());
                    return orderItem;
                })
                .toList());
    }

    private BigDecimal calculateTotal(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CheckoutResponse mapToResponse(Order order) {
        return CheckoutResponse.builder()
                .orderId(order.getId())
                .deliveryMethod(order.getDeliveryMethod())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .pickupStoreName(order.getPickupStoreName())
                .receiverName(order.getReceiverName())
                .mercadoPagoPreferenceId(order.getMercadoPagoPreferenceId())
                .mercadoPagoInitPoint(order.getMercadoPagoInitPoint())
                .mercadoPagoSandboxInitPoint(order.getMercadoPagoSandboxInitPoint())
                .shippingAddress(mapAddress(order.getShippingAddress()))
                .items(order.getOrderItems().stream().map(this::mapItem).toList())
                .build();
    }

    private CheckoutAddressResponse mapAddress(Address address) {
        if (address == null) {
            return null;
        }

        return CheckoutAddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .number(address.getNumber())
                .apartment(address.getApartment())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .reference(address.getReference())
                .build();
    }

    private CheckoutItemResponse mapItem(OrderItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return CheckoutItemResponse.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(subtotal)
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

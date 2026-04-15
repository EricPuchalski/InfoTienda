package com.infotienda.cart.service;

import com.infotienda.core.constant.CookieConstants;
import com.infotienda.core.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuestSessionService {

    private final CookieUtil cookieUtil;

    @Value("${application.security.guest-session.cookie-secure:true}")
    private boolean guestSessionCookieSecure;

    public String resolveOrCreateGuestSessionId(HttpServletRequest request, HttpServletResponse response) {
        return readGuestSessionId(request).orElseGet(() -> createGuestSession(request, response));
    }

    public Optional<String> readGuestSessionId(HttpServletRequest request) {
        return cookieUtil.readCookie(request, CookieConstants.GUEST_SESSION_COOKIE_NAME)
                .filter(this::isValidUuid);
    }

    public void clearGuestSession(HttpServletRequest request, HttpServletResponse response) {
        cookieUtil.clearCookie(
                response,
                CookieConstants.GUEST_SESSION_COOKIE_NAME,
                true,
                shouldUseSecureCookie(request),
                "Strict"
        );
    }

    private String createGuestSession(HttpServletRequest request, HttpServletResponse response) {
        String guestSessionId = UUID.randomUUID().toString();
        cookieUtil.createCookie(
                response,
                CookieConstants.GUEST_SESSION_COOKIE_NAME,
                guestSessionId,
                CookieConstants.GUEST_SESSION_COOKIE_MAX_AGE_SECONDS,
                true,
                shouldUseSecureCookie(request),
                "Strict"
        );
        return guestSessionId;
    }

    private boolean shouldUseSecureCookie(HttpServletRequest request) {
        if (!guestSessionCookieSecure) {
            return false;
        }

        if (request.isSecure()) {
            return true;
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && "https".equalsIgnoreCase(forwardedProto);
    }

    private boolean isValidUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}

package com.infotienda.core.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@Component
public class CookieUtil {


    public void createCookie(HttpServletResponse response, String name, String value, int maxAge) {
        createCookie(response, name, value, maxAge, true, true, "None");
    }

    public void createCookie(
            HttpServletResponse response,
            String name,
            String value,
            int maxAge,
            boolean httpOnly,
            boolean secure,
            String sameSite
    ) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(maxAge);

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void clearCookie(HttpServletResponse response, String name) {
        clearCookie(response, name, true, true, "None");
    }

    public void clearCookie(
            HttpServletResponse response,
            String name,
            boolean httpOnly,
            boolean secure,
            String sameSite
    ) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .httpOnly(httpOnly)
                .secure(secure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(0);

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

}

package com.infotienda.security.service;

import com.infotienda.exception.ResourceNotFoundException;
import com.infotienda.model.AuthProvider;
import com.infotienda.model.Role;
import com.infotienda.model.User;
import com.infotienda.security.dto.LoginRequest;
import com.infotienda.security.dto.RegisterRequest;
import com.infotienda.security.model.CustomUserDetails;
import com.infotienda.security.repository.UserRepository;
import com.infotienda.constant.CookieConstants;
import com.infotienda.util.CookieUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CookieUtil cookieUtil;

    public void register(RegisterRequest request, HttpServletResponse response) {
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .build();
        var savedUser = repository.save(user);
        generateAndSetTokens(response, new CustomUserDetails(savedUser));
    }

    public void authenticate(LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        generateAndSetTokens(response, new CustomUserDetails(user));
    }

    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.readCookie(request, CookieConstants.REFRESH_TOKEN_COOKIE_NAME)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        String userEmail = extractUsernameOrThrow(refreshToken);
        var user = this.repository.findByEmail(userEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        if (!isRefreshTokenValid(refreshToken, userDetails)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String accessToken = jwtService.generateToken(userDetails);
        String rotatedRefreshToken = jwtService.generateRefreshToken(userDetails);
        user.setRefreshToken(rotatedRefreshToken);
        repository.save(user);

        cookieUtil.createCookie(response, CookieConstants.ACCESS_TOKEN_COOKIE_NAME, accessToken, jwtService.getJwtExpiration());
        cookieUtil.createCookie(response, CookieConstants.REFRESH_TOKEN_COOKIE_NAME, rotatedRefreshToken, jwtService.getRefreshExpiration());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookieUtil.readCookie(request, CookieConstants.REFRESH_TOKEN_COOKIE_NAME)
                .ifPresent(this::revokeRefreshTokenIfMatches);
        cookieUtil.clearCookie(response, CookieConstants.ACCESS_TOKEN_COOKIE_NAME);
        cookieUtil.clearCookie(response, CookieConstants.REFRESH_TOKEN_COOKIE_NAME);
    }

    private void generateAndSetTokens(HttpServletResponse response, CustomUserDetails userDetails) {
        var jwtToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);

        var user = repository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRefreshToken(refreshToken);
        repository.save(user);

        cookieUtil.createCookie(response, CookieConstants.ACCESS_TOKEN_COOKIE_NAME, jwtToken, jwtService.getJwtExpiration());
        cookieUtil.createCookie(response, CookieConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken, jwtService.getRefreshExpiration());
    }

    private void revokeRefreshTokenIfMatches(String refreshToken) {
        try {
            String userEmail = jwtService.extractUsername(refreshToken);
            if (userEmail == null) {
                return;
            }
            repository.findByEmail(userEmail).ifPresent(user -> {
                if (refreshToken.equals(user.getRefreshToken())) {
                    user.setRefreshToken(null);
                    repository.save(user);
                }
            });
        } catch (JwtException | IllegalArgumentException ignored) {
            // Ignore invalid token data and continue clearing client-side cookies.
        }
    }

    private String extractUsernameOrThrow(String refreshToken) {
        try {
            String username = jwtService.extractUsername(refreshToken);
            if (username == null) {
                throw new BadCredentialsException("Invalid refresh token");
            }
            return username;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    private boolean isRefreshTokenValid(String refreshToken, CustomUserDetails userDetails) {
        try {
            return jwtService.isTokenValid(refreshToken, userDetails);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}

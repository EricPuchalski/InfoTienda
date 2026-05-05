package com.infotienda.security.config;

import com.infotienda.security.model.Role;
import com.infotienda.security.filter.AuthRateLimitFilter;
import com.infotienda.security.filter.JwtAuthenticationFilter;
import com.infotienda.security.handler.RestAccessDeniedHandler;
import com.infotienda.security.handler.RestAuthenticationEntryPoint;
import com.infotienda.security.model.CustomOAuth2User;
import com.infotienda.security.model.CustomUserDetails;
import com.infotienda.security.repository.UserRepository;
import com.infotienda.security.service.CustomOAuth2UserService;
import com.infotienda.security.service.JwtService;
import com.infotienda.cart.service.CartService;
import com.infotienda.cart.service.GuestSessionService;
import com.infotienda.core.constant.CookieConstants;
import com.infotienda.core.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CustomOAuth2UserService customOauth2UserService;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final GuestSessionService guestSessionService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/v1/payments/mercado-pago/webhook")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/csrf",
                                "/login",
                                "/login/oauth2/**",
                                "/oauth2/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**", "/api/v1/categories").permitAll()
                        .requestMatchers("/api/v1/admin/**", "/api/v1/categories/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers("/api/v1/user").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                        .requestMatchers("/api/v1/cart/**").permitAll()
                        .requestMatchers("/api/v1/payments/mercado-pago/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/products/**"
                        ).hasAuthority(Role.ADMIN.name())
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/products/**"
                        ).hasAuthority(Role.ADMIN.name())
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authRateLimitFilter, JwtAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOauth2UserService)
                        )
                        .successHandler(oauth2SuccessHandler())
                );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler() {
        return (request, response, authentication) -> {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            CustomUserDetails userDetails = new CustomUserDetails(oauth2User.getUser());

            String jwtToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            var user = oauth2User.getUser();
            user.setRefreshToken(refreshToken);
            userRepository.save(user);

            cookieUtil.createCookie(response, CookieConstants.ACCESS_TOKEN_COOKIE_NAME, jwtToken, jwtService.getJwtExpiration());
            cookieUtil.createCookie(response, CookieConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken, jwtService.getRefreshExpiration());
            guestSessionService.readGuestSessionId(request).ifPresent(guestSessionId -> {
                try {
                    cartService.mergeGuestSessionCartIntoUser(guestSessionId, user.getEmail());
                    guestSessionService.clearGuestSession(request, response);
                } catch (RuntimeException ex) {
                    log.warn("Guest cart merge failed on OAuth2 login for userEmail={} guestSessionId={}: {}", user.getEmail(), guestSessionId, ex.getMessage());
                }
            });

            response.sendRedirect("http://localhost:5173/");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Set-Cookie"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

package com.infotienda.security.service;

import com.infotienda.security.model.AuthProvider;
import com.infotienda.security.model.Role;
import com.infotienda.security.model.User;
import com.infotienda.security.model.CustomOAuth2User;
import com.infotienda.security.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        Optional<User> userOptional = userRepository.findByEmail(email);

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getProvider() == AuthProvider.LOCAL) {
                user.setProvider(AuthProvider.GOOGLE);
            }
            user.setFirstName(oidcUser.getAttribute("given_name"));
            user.setLastName(oidcUser.getAttribute("family_name"));
        } else {
            user = User.builder()
                    .email(email)
                    .firstName(oidcUser.getAttribute("given_name"))
                    .lastName(oidcUser.getAttribute("family_name"))
                    .role(Role.USER)
                    .provider(AuthProvider.GOOGLE)
                    .createdAt(LocalDateTime.now())
                    .build();
        }
        userRepository.save(user);

        return new CustomOAuth2User(oidcUser, user);
    }
}

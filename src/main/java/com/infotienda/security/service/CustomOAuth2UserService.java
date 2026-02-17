package com.infotienda.security.service;

import com.infotienda.model.AuthProvider;
import com.infotienda.model.Role;
import com.infotienda.model.User;
import com.infotienda.security.model.CustomOAuth2User;
import com.infotienda.security.repository.UserRepository;
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
                // Here you can throw an exception or handle the case where the user
                // already exists with a local account. For simplicity, we'll update the user.
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
                    .build();
        }
        userRepository.save(user);

        return new CustomOAuth2User(oidcUser, user);
    }
}

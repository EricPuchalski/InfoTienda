package com.infotienda.security.mapper;

import com.infotienda.security.dto.UserResponse;
import com.infotienda.security.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toDto(User user){
        return UserResponse.builder()
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }


}

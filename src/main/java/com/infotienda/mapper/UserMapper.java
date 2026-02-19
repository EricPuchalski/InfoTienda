package com.infotienda.mapper;

import com.infotienda.dto.UserResponse;
import com.infotienda.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toDto(User user){
        return UserResponse.builder()
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .email(user.getEmail())
                .build();
    }


}

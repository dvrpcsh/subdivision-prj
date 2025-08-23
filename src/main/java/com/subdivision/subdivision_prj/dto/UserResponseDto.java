package com.subdivision.subdivision_prj.dto;

import com.subdivision.subdivision_prj.domain.User;
import lombok.Getter;

@Getter
public class UserResponseDto {

    private String email;
    private String nickname;

    public UserResponseDto(User user) {
        this.email = user.getEmail();
        this.nickname = user.getNickname();
    }
}

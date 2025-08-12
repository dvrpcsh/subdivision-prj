package com.subdivision.subdivision_prj.dto;

import com.subdivision.subdivision_prj.domain.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequestDto {
    private String email;
    private String password;
    private String nickname;

    // DTO를 User 엔티티로 변환하는 메서드입니다.
    // 비밀번호는 서비스 단에서 암호화 될 예정이므로 여기서는 평문으로 받습니다.
    public User toEntity() {
        return User.builder()
                .email(email)
                .password(password) //임시로 평문 저장, 서비스에서 암호화 후 다시 세팅할 예정
                .nickname(nickname)
                .build();
    }
}

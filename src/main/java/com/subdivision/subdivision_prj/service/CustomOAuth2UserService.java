package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.User;
import com.subdivision.subdivision_prj.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String)attributes.get("email");
        String name = (String)attributes.get("name");

        Optional<User> userOptional = userRepository.findByEmail(email);

        if(userOptional.isEmpty()) {
            //신규 사용자일 경우, DB에 저장
            String nickname = generateUniqueNickname(name);
            User newUser = User.builder()
                    .email(email)
                    .nickname(nickname)
                    //SNS로그인 사용자는 비밀번호가 없으므로 임의의 값 저장
                    .password("OAUTH2_USER_PASSWORD")
                    .build();
            userRepository.save(newUser);
        }

        return oAuth2User;
    }

    private String generateUniqueNickname(String baseName) {
        String nickname = baseName;
        while(userRepository.findByNickname(nickname).isPresent()) {
            int randomNumber = new Random().nextInt(9999999) + 1;
            nickname = baseName + randomNumber;
        }

        return nickname;
    }
}

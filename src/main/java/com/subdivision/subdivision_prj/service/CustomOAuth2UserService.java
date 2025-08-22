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

        //어떤 SNS 제공자인지 확인합니다.
        String provider = userRequest.getClientRegistration().getRegistrationId();

        String email = null;
        String name = null;

        //제공자에 따라 데이터를 파싱하는 방식을 분기합니다.
        if(provider.equals("google")) {
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        } else if(provider.equals("kakao")) {
            //카카오 응답은 'kakao_account'안에, 그 안에 'profile'이 있는 중첩 구조입니다.
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            //이메일은 사용자가 동의했을 경우에만 제공되므로, null일 수 있습니다.
            if(kakaoAccount.containsKey("email")) {
                email = (String)kakaoAccount.get("email");
            }
            //이름 대신 'nickname'을 사용합니다.
            name = (String)profile.get("nickname");
        }

        //이메일이 없는 경우(카카오 비즈앱 미전환 등), 카카오의 고유 ID를 이용해 임시 이메일을 생성합니다.
        if(email == null) {
            //oAuth2User.getName()은 해당 SNS 고유 ID를 반환합니다.
            email = oAuth2User.getName() + "@" + provider + ".com";
        }

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

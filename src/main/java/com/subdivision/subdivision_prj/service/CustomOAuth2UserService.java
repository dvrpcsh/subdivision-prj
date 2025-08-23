package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.User;
import com.subdivision.subdivision_prj.domain.UserRepository;
import com.sun.tools.jconsole.JConsoleContext;
import com.sun.tools.jconsole.JConsolePlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        //어떤 SNS 제공자인지 확인합니다.
        String provider = userRequest.getClientRegistration().getRegistrationId();

        String email = null;
        String name = null;
        String id = null;

        //제공자에 따라 데이터를 파싱하는 방식을 분기합니다.
        if(provider.equals("google")) {
            id = oAuth2User.getAttribute("sub");
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        } else if(provider.equals("kakao")) {
            log.info("--- KAKAO DATA TYPE DEBUG ---");

            Object idObject = oAuth2User.getAttribute("id");
            log.info("'id' value: {}, type:{}", idObject, idObject.getClass().getName());
            id = String.valueOf(idObject);

            Object propertiesObject = oAuth2User.getAttribute("properties");
            log.info("'properties' value: {}, type: {}", propertiesObject, propertiesObject.getClass().getName());
            Map<String, Object> properties = (Map<String, Object>) propertiesObject;

            Object nicknameObject = properties.get("nickname");
            log.info("'nickname' value: {}, type: {}", nicknameObject, nicknameObject.getClass().getName());
            name = (String)nicknameObject;

            log.info("--------------------");

            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            if(kakaoAccount != null && kakaoAccount.containsKey("email")) {
                email = (String)kakaoAccount.get("email");
            }
        } else if(provider.equals("naver")) {
            //네이버 응답 파싱 로직
            Map<String, Object> response = oAuth2User.getAttribute("response");
            id = (String)response.get("id");
            email = (String)response.get("email");
            name = (String)response.get("nickname");
        }

        //이메일이 없는 경우(카카오 비즈앱 미전환 등), 카카오의 고유 ID를 이용해 임시 이메일을 생성합니다.
        if(email == null) {
            //oAuth2User.getName()은 해당 SNS 고유 ID를 반환합니다.
            email = id + "@" + provider + ".com";
        }

        Optional<User> userOptional = userRepository.findByEmail(email);

        if(userOptional.isEmpty()) {
            //generateUniqueNickname에 넘겨주기 전에, name이 null일 경우를 대비하고
            //다른 타입일 가능성을 원천 차단하기 위해 String.valueOf()를 한 번 더 사용합니다.
            //신규 사용자일 경우, DB에 저장
            String nicknameSource = (name != null) ? name : provider + "_" + id;
            String nickname = generateUniqueNickname(String.valueOf(nicknameSource));

            //User.builder()에 모든 필수 필드가 포함되었느지 확인합니다.
            //만약 User엔티티에 다른 not-null 필드가 있다면, 여기에 기본값을 설정해야 합니다.
            User newUser = User.builder()
                    .email(email)
                    .nickname(nickname)
                    //SNS로그인 사용자는 비밀번호가 없으므로 임의의 값 저장
                    .password("OAUTH2_USER_PASSWORD")
                    .build();
            userRepository.save(newUser);
        }

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2User.getAttributes(),
                // 이 부분의 키 값은 application.properties에 설정된 user-name-attribute와 일치해야 합니다.
                userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
        );
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

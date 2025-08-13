package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.User;
import com.subdivision.subdivision_prj.domain.UserRepository;
import com.subdivision.subdivision_prj.dto.SignUpRequestDto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @SpringBootTest: 실제 에플리케이션을 실행하는 것 처럼 모든 Bean을 로드하여 톱합테스트를 진행합니다.
 * @Transactional: 테스트가 끝난 후 모든 데이터베이스 변경사항을 롤백합니다.
 * 이를 통해 각 테스트는 서로에게 영향을 주지 않고 독립적으로 시행됩니다.
 */
@SpringBootTest
@Transactional
public class AuthServiceTest {

    //@Autowired: Spring 컨테이너가 관리하는 Bean을 자동으로 주입받습니다.
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입에 성공한다")
    void join_success() {
        //given - 테스트를 위한 데이터 준비
        SignUpRequestDto requestDto = new SignUpRequestDto();
        requestDto.setEmail("test@example.com");
        requestDto.setPassword("password123");
        requestDto.setNickname("테스트유저");

        //when - 테스트하려는 실제 로직 실행
        Long savedUserId = authService.signup(requestDto);

        //then - 실행 결과 검증
        User foundUser = userRepository.findById(savedUserId)
                .orElseThrow(()-> new AssertionError("저장된 유저를 찾을 수 없습니다."));

        assertThat(foundUser.getEmail()).isEqualTo(requestDto.getEmail());
        assertThat(passwordEncoder.matches(requestDto.getPassword(), foundUser.getPassword())).isTrue();

    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 시 예외가 발생한다")
    void join_fail() {
        //given - 먼저 사용자 한 명을 저장해서 중복 조건을 만듭니다.
        userRepository.save(User.builder()
                .email("test@example.com")
                .password("anypassword")
                .nickname("기존유저")
                .build());

        SignUpRequestDto requestDto = new SignUpRequestDto();
        requestDto.setEmail("test@example.com");
        requestDto.setPassword("password123");
        requestDto.setNickname("새로운유저");

        //when & then - signup 메서드 실행 시 IllegalArgumentException이 발생하는지 검증
        assertThrows(IllegalArgumentException.class, () -> {
            authService.signup(requestDto);
        });
    }
}

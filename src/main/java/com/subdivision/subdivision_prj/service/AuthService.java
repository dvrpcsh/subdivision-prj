package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.User;
import com.subdivision.subdivision_prj.domain.UserRepository;
import com.subdivision.subdivision_prj.dto.SignUpRequestDto;
import com.subdivision.subdivision_prj.dto.LoginRequestDto;
import com.subdivision.subdivision_prj.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service //이 클래스가 비즈니스 로직을 처리하는 서비스 계층임을 나타냅니다.
@RequiredArgsConstructor // final 필드나 @NonNull 필드에 대한 생성자를 자동으로 만들어줍니다. (의존성 주입)
public class AuthService {

    //final 키워드를 사용하여 의존성을 주입받습니다. (생성자 주입 방식)
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    /**
     * 닉네임 중복 여부를 확인합니다.
     * @param nickname 중복 확인할 닉네임
     * @return 중복이면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean checkNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 이메일 중복 여부를 확인합니다.
     * @param email 중복 확인할 이메일
     * @return 중복이면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    // @Transactional: 이 메서드 내에서 일어나는 모든 DB 작업이 하나의 트랜잭션으로 묶입니다.
    // 성공적으로 끝나면 커밋(commit), 예외 발생 시 롤백(rollback) 처리합니다.
    @Transactional
    public Long signup(SignUpRequestDto requestDto) {
        // 1.이메일 중복 확인
        // 만약 userRepository.findByEmail을 통해 조회한 이메일이 이미 존재한다면, 예외를 발생시킵니다.
        if(userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2.비밀번호 암호화
        // 클라이언트로부터 받은 평문 비밀번호를 PasswordEncoder를 이용해 안전하게 해시값으로 변환합니다.
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        // 3.User 엔티티 생성
        // DTO를 엔티티로 변환하되, 암호화된 비밀번호를 사용합니다.
        User user = User.builder()
                .email(requestDto.getEmail())
                .password(encodedPassword)
                .nickname(requestDto.getNickname())
                .build();

        // 4.데이터베이스에 저장
        // 완성된 User 엔티티를  repository를 통해 데이터베이스에 저장합니다.
        User savedUser = userRepository.save(user);

        // 5.저장된 사용자의 ID를 반환합니다.
        return savedUser.getId();
    }

    /**
     * 로그인 메서드
     * @param requestDto 로그인 요청 DTO
     * @return 생성된 JWT
     */
    public String login(LoginRequestDto requestDto) {
        //1.이메일로 사용자 조회
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        //2.비밀번호 일치 여부 확인
        if(!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }

        //3.비밀번호가 일치하면 JWT생성하여 반환
        return jwtTokenProvider.createToken(user.getEmail());
    }
}

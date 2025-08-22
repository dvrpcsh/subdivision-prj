package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.User;
import com.subdivision.subdivision_prj.domain.UserRepository;
import com.subdivision.subdivision_prj.dto.SignUpRequestDto;
import com.subdivision.subdivision_prj.dto.LoginRequestDto;
import com.subdivision.subdivision_prj.config.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.RequiredArgsConstructor;

import java.util.Random;
import java.util.concurrent.TimeUnit;


@Service //이 클래스가 비즈니스 로직을 처리하는 서비스 계층임을 나타냅니다.
@RequiredArgsConstructor // final 필드나 @NonNull 필드에 대한 생성자를 자동으로 만들어줍니다. (의존성 주입)
public class AuthService {

    //final 키워드를 사용하여 의존성을 주입받습니다. (생성자 주입 방식)
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 지정된 이메일로 6자리 인증 코드를 생성하여 발송합니다.
     * 생성된 코드는 Redis에 5분간 저장됩니다.
     * @param email 인증 코드를 받을 이메일 주소
     */
    public void sendVerificationCode(String email) {
        //1.6자리 랜덤 숫자 인증 코드 생성
        String code = createRandomCode();

        //2.이메일 발송
        emailService.sendVerificationCode(email, code);

        //3.Redis에 인증 코드 저장(key: 이메일, value: 코드, 유효시간: 5분)
        redisTemplate.opsForValue().set(email, code, 5, TimeUnit.MINUTES);
    }

    /**
     * 6자리 랜덤 숫자 코드를 생성하는 private 헬퍼 메서드
     * @return 6자리 문자 숫자열
     */
    private String createRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); //100000 ~ 999999

        return String.valueOf(code);
    }

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
        String email = requestDto.getEmail();
        String isVerified = (String) redisTemplate.opsForValue().get("VERIFIED_" + email);

        // 1.이메일 중복 확인
        // 만약 userRepository.findByEmail을 통해 조회한 이메일이 이미 존재한다면, 예외를 발생시킵니다.
        if(userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        //2.이메일에 대한 인증 코드 검증
        if(isVerified == null || !isVerified.equals("true")) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        //3.비밀번호 암호화
        //클라이언트로부터 받은 평문 비밀번호를 PasswordEncoder를 이용해 안전하게 해시값으로 변환합니다.
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        //4.User 엔티티 생성
        //DTO를 엔티티로 변환하되, 암호화된 비밀번호를 사용합니다.
        User user = User.builder()
                .email(requestDto.getEmail())
                .password(encodedPassword)
                .nickname(requestDto.getNickname())
                .build();

        //5.데이터베이스에 저장
        //완성된 User 엔티티를  repository를 통해 데이터베이스에 저장합니다.
        User savedUser = userRepository.save(user);

        //6.저장된 사용자의 ID를 반환합니다.
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

    /**
     * 회원가입 시 입력한 이메일로 인증코드를 보내어 유효한 코드인지 검증합니다.
     * @param email 검증할 이메일
     * @param code 사용자가 입력한 인증 코드
     * @return 검증 성공 여부 (true/false)
     */
    public boolean verifyCode(String email, String code) {
        //Redis에서 해당 이메일을 key로 저장된 인증 코드를 가져옵니다.
        String storedCode = (String) redisTemplate.opsForValue().get(email);

        //Redis에 코드가 존재하고, 사용자가 입력한 코드와 일치하는지 확인합니다.
        if(storedCode != null && storedCode.equals(code)) {
            //인증 성공 시, Redis에서 해당 코드를 즉시 삭제합니다.
            redisTemplate.delete(email);

            //인증 성공 상태를 Redis에 10분간 저장
            redisTemplate.opsForValue().set("VERIFIED_" + email, "true", 10, TimeUnit.MINUTES);

            return true;
        }
        return false;
    }
}

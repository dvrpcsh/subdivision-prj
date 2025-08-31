package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.User;
import com.subdivision.subdivision_prj.domain.UserRepository;
import com.subdivision.subdivision_prj.dto.SignUpRequestDto;
import com.subdivision.subdivision_prj.dto.LoginRequestDto;
import com.subdivision.subdivision_prj.config.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 회원가입, 로그인, 이메일 인증 등 사용자 인증 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * @author subdivision
 */
@Slf4j // Lombok 어노테이션으로, log.info(), log.error() 등을 편리하게 사용할 수 있게 해줍니다.
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService; // local 환경에서는 주입되지 않고 null이 될 수 있습니다.
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * AuthService의 생성자입니다. 의존성 주입(Dependency Injection)을 담당합니다.
     * @param emailService @Autowired(required = false)를 통해 'prod' 환경에서는 실제 EmailService Bean이 주입되고,
     * 'local' 환경에서는 Bean이 없어도 오류 없이 null이 주입됩니다.
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, @Autowired(required = false) EmailService emailService, RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 지정된 이메일로 6자리 인증 코드를 생성하여 발송(또는 로그 출력)합니다.
     * 생성된 코드는 Redis에 5분간 저장됩니다.
     * @param email 인증 코드를 받을 이메일 주소
     */
    public void sendVerificationCode(String email) {
        // 1. 6자리 랜덤 인증 코드를 생성합니다.
        String code = createRandomCode();

        // 2. 생성된 인증 코드를 Redis에 저장합니다. Key는 이메일, Value는 코드이며 5분간 유효합니다.
        redisTemplate.opsForValue().set(email, code, 5, TimeUnit.MINUTES);

        // 3. [환경 분기] emailService 객체가 주입되었는지 확인합니다.
        if (emailService != null) {
            // 'prod' 환경: 실제 EmailService를 통해 이메일을 발송합니다.
            emailService.sendVerificationCode(email, code);
            log.info("{}로 실제 인증 코드가 발송되었습니다.", email);
        } else {
            // 'local'(Docker) 환경: emailService가 null이므로, 실제 이메일 대신 Docker 로그에 인증 코드를 출력합니다.
            // 개발자는 이 로그를 보고 인증번호를 확인하여 회원가입을 테스트할 수 있습니다.
            log.info("EmailService 비활성화됨. [개발용] 이메일: {}, 인증 코드: {}", email, code);
        }
    }

    /**
     * 6자리 랜덤 숫자 코드를 생성하는 private 헬퍼 메서드입니다.
     * @return 6자리 숫자 문자열
     */
    private String createRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999 범위의 숫자 생성
        return String.valueOf(code);
    }

    /**
     * 닉네임이 이미 데이터베이스에 존재하는지 확인합니다.
     * @param nickname 중복 확인할 닉네임
     * @return 중복이면 true, 아니면 false
     */
    @Transactional(readOnly = true) // 데이터 변경이 없는 조회 작업이므로 readOnly=true로 성능을 최적화합니다.
    public boolean checkNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 이메일이 이미 데이터베이스에 존재하는지 확인합니다.
     * @param email 중복 확인할 이메일
     * @return 중복이면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 사용자로부터 받은 정보로 회원가입을 처리합니다.
     * @param requestDto 회원가입 요청 정보를 담은 DTO
     * @return 생성된 사용자의 ID
     */
    @Transactional // 데이터베이스에 변경이 발생하는 작업이므로 트랜잭션을 적용합니다.
    public Long signup(SignUpRequestDto requestDto) {
        String email = requestDto.getEmail();
        String isVerified = (String) redisTemplate.opsForValue().get("VERIFIED_" + email);

        // 이메일이 이미 존재하는지 다시 한번 확인하여 중복 가입을 방지합니다.
        if(userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 이메일 인증 완료 여부를 확인합니다.
        if(isVerified == null || !isVerified.equals("true")) {
            // 'local' 환경에서는 이메일 발송이 안되므로, 개발 편의를 위해 인증을 통과시켜주는 로직입니다.
            if(emailService == null) {
                log.warn("EmailService가 없어 이메일 인증을 통과 처리합니다. Email: {}", email);
            } else {
                // 'prod' 환경에서는 이메일 인증이 반드시 필요합니다.
                throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
            }
        }

        // 클라이언트로부터 받은 평문 비밀번호를 Bcrypt를 이용해 안전하게 해시값으로 변환합니다.
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        // DTO를 User 엔티티로 변환하여 데이터베이스에 저장할 준비를 합니다.
        User user = User.builder()
                .email(requestDto.getEmail())
                .password(encodedPassword)
                .nickname(requestDto.getNickname())
                .build();

        User savedUser = userRepository.save(user);

        return savedUser.getId();
    }

    /**
     * 이메일과 비밀번호로 로그인을 처리하고, 성공 시 JWT를 발급합니다.
     * @param requestDto 로그인 요청 정보를 담은 DTO
     * @return 생성된 JWT 문자열
     */
    public String login(LoginRequestDto requestDto) {
        // 1. 이메일로 사용자를 조회합니다. 없으면 예외를 발생시킵니다.
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 2. 저장된 암호화된 비밀번호와 사용자가 입력한 평문 비밀번호를 비교합니다.
        if(!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }

        // 3. 비밀번호가 일치하면, 해당 사용자의 이메일을 기반으로 JWT를 생성하여 반환합니다.
        return jwtTokenProvider.createToken(user.getEmail());
    }

    /**
     * 사용자가 입력한 인증 코드가 Redis에 저장된 코드와 일치하는지 검증합니다.
     * @param email 검증할 이메일
     * @param code 사용자가 입력한 인증 코드
     * @return 검증 성공 시 true, 실패 시 false
     */
    public boolean verifyCode(String email, String code) {
        String storedCode = (String) redisTemplate.opsForValue().get(email);

        if(storedCode != null && storedCode.equals(code)) {
            // 인증 성공 시, 보안을 위해 사용된 인증 코드는 즉시 삭제합니다.
            redisTemplate.delete(email);
            // 이메일 인증이 완료되었음을 10분간 Redis에 저장하여 signup 단계에서 확인할 수 있도록 합니다.
            redisTemplate.opsForValue().set("VERIFIED_" + email, "true", 10, TimeUnit.MINUTES);
            return true;
        }
        return false;
    }
}


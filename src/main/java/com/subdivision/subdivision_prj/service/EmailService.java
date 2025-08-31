package com.subdivision.subdivision_prj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 이 서비스는 'prod' 프로파일과 같이 실제 이메일 발송이 필요한 환경에서만 활성화됩니다.
 * @author subdivision
 */
@Service
@RequiredArgsConstructor
// 💡 [핵심] @ConditionalOnProperty 어노테이션은 이 Bean의 생성 여부를 결정하는 '스마트 스위치'입니다.
// application.properties 파일에 'spring.mail.enabled'라는 속성이 있고,
// 그 값이 'true'일 경우에만 Spring 컨테이너가 EmailService를 생성합니다.
// 'local' 환경에서는 이 속성이 없으므로, 이 서비스는 로드되지 않아 NullPointerException을 방지합니다.
@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "true")
public class EmailService {

    // Spring Boot Mail Starter가 자동으로 구성해주는 이메일 발송용 핵심 인터페이스입니다.
    private final JavaMailSender javaMailSender;

    /**
     * 지정된 이메일 주소로 인증 코드를 담은 메일을 발송합니다.
     * @param toEmail 수신자 이메일 주소
     * @param code    발송할 6자리 인증 코드
     */
    public void sendVerificationCode(String toEmail, String code) {
        // 1. 발송할 이메일의 기본 내용을 구성합니다. (수신자, 제목, 본문)
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject("[우리동네공동구매] 회원가입 이메일 인증 코드입니다.");
        mailMessage.setText("회원가입을 완료하려면 아래 인증 코드를 입력해주세요.\n\n인증 코드: " + code);

        // 2. 구성된 메시지를 JavaMailSender를 통해 실제 SMTP 서버로 전송합니다.
        javaMailSender.send(mailMessage);
    }
}

package com.subdivision.subdivision_prj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject("[우리동네공동구매] 회원가입 이메일 인증 코드입니다.");
        mailMessage.setText("인증 코드: "+ code);

        javaMailSender.send(mailMessage);
    }
}

package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .map(this::createUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException(username + " -> 데이터베이스에서 찾을 수 없습니다."));
    }

    // DB에 User값이 존재한다면 UserDetails 객체로 만들어서 리턴
    private UserDetails createUserDetails(com.subdivision.subdivision_prj.domain.User user) {
        return new User(
                user.getEmail(),
                user.getPassword(),
                Collections.emptyList() // 우선 간단하게 권한은 없는 상태로
        );
    }
}

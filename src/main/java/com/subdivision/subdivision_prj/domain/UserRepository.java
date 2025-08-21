package com.subdivision.subdivision_prj.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository를 상속받으면 기본적인 CRUD(생성, 조회, 수정, 삭제) 메서드가 자동으로 제공됩니다.
// <엔티티 클래스, 엔티티의 ID 필드 타입>
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA는 메서드 이름을 분석하여 자동으로 쿼리를 생성해줍니다.
    // 'findByEmail'은 이메일 주소로 사용자를 찾아오는 쿼리를 만들어줍니다.
    // Optional<User>는 사용자가 존재할 수도, 존재하지 않을 수도 있는 경우 안전하게 처리하기 위해 사용합니다.
    Optional<User> findByEmail(String email);

    //닉네임으로 사용자가 존재하는지 확인하는 메서드
    boolean existsByNickname(String nickname);

    //이메일로 사용자가 존재하는지 확인하는 메서드
    boolean existsByEmail(String email);
}

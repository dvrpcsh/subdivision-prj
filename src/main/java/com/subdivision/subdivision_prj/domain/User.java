package com.subdivision.subdivision_prj.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//Lombok 어노테이션: 코드를 간결하게 만들어 줍니다.
@Getter //각 필드 값을 조회할 수 있는 getter 메서드를 자동으로 생성합니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) //JPA는 기본 생성자를 필요로 합니다. PROTECTED로 안전하게 설정합니다.

//JPA 어노테이션: 이 클래스가 데이터베이스 테이블과 매핑되는 엔티티 클래스임을 나타냅니다.
@Entity
@Table(name = "users") //데이터베이스에서 'users'라는 이름의 테이블과 매핑됩니다.
public class User {

    @Id //이 필트가 테이블의 기본 키(Primary Key)임을 나타냅니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) //기본 키 값을 데이터베이스가 자동으로 생성(auto-increment)하도록 설정합니다.
    @Column(name = "user_id") //'null'을 허용하지 않고, 값이 중복되지 않도록 유니크 제약조건을 설정합니다.
    private Long id;

    @Column(nullable = false, unique = true) //'null'을 허용하지 않고, 값이 중복되지 않도록 유니크 제약조건을 설정합니다.
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    //빌더 패턴: 객체 생성을 더 명확하고 유연하게 할 수 있도록 돕습니다.
    @Builder
    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}

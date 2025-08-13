package com.subdivision.subdivision_prj.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JpaRepository<T,ID> 인터페이스를 상속받습니다.
 * T: 이 레파지토리가 다룰 엔티티 클래스 (여기서는 Pot)
 * ID: 해당 엔티티의 ID 필드 타입 (Pot 엔티티의 id는 Long 타입)
 */
public interface PotRepository extends JpaRepository<Pot, Long> {
    //기본적인 CRUD 메서드(save, findById, findAll, delete 등)는
    //JpaRepository에 이미 구현되어 있으므로 따로 작성할 필요가 없습니다.
}

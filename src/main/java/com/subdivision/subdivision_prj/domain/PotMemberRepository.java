package com.subdivision.subdivision_prj.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PotMemberRepository extends JpaRepository <PotMember, Long> {
    //특정 사용자가 특정 팟에 이미 참여했는지 확인하기 위한 메서드
    Optional<PotMember> findByPotAndUser(Pot pot, User user);

    //User 엔티티를 기준으로 모든 참여 정보를 조회하는 메서드
    List<PotMember> findAllByUser(User user);

    //특정 팟(Pot)과 사용자(User)에 해당하는 PotMember 엔티티의 개수를 반환합니다.
    long countByPotAndUser(Pot pot, User user);

}

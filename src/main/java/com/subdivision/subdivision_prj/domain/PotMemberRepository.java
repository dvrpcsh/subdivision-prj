package com.subdivision.subdivision_prj.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PotMemberRepository extends JpaRepository <PotMember, Long> {
    //특정 사용자가 특정 팟에 이미 참여했는지 확인하기 위한 메서드
    Optional<PotMember> findByPotAndUser(Pot pot, User user);
}

package com.subdivision.subdivision_prj.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * JpaRepository<T,ID> 인터페이스를 상속받습니다.
 * T: 이 레파지토리가 다룰 엔티티 클래스 (여기서는 Pot)
 * ID: 해당 엔티티의 ID 필드 타입 (Pot 엔티티의 id는 Long 타입)
 */
public interface PotRepository extends JpaRepository<Pot, Long> {
    //기본적인 CRUD 메서드(save, findById, findAll, delete 등)는
    //JpaRepository에 이미 구현되어 있으므로 따로 작성할 필요가 없습니다.

    /**
     * PostgreSQL (PostGIS)를 사용하여 특정 지점 반경 내의 Pot을 검색합니다.
     * ST_DWithin 함수는 거리 계싼과 범위 조회를 한 번에 처리하며, 공간 인덱스를 활용하여 성능이 매우 뛰어납니다.
     *
     * @param lon      중심점의 경도(longitude)
     * @param lat      중심점의 위도(latitude)
     * @param distance 검색 반경(km단위)
     * @return 검색 조건에 맞는 Pot 엔티티 리스트
     * @ST_DWithin: 두 지점 사이의 지정된 거리 이내인지 boolean으로 반환하는 함수
     * @ST_SetSRID(ST_MakePoint(경도,위도), 4326): 경도, 위도 값을 SRID 4326(WGS84) 좌표계의 geometry 타입 포인트로 생성
     * @::geography: geometry 타입을 구면 거리 계산에 적합한 geography 타입으로 형변환
     */
    @Query(value = "SELECT * FROM pots p WHERE ST_DWithin(" +
            "ST_SetSRID(ST_MakePoint(p.longitude, p.latitude), 4326)::geography, " +
            "ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, " +
            ":distance * 1000)",
            nativeQuery = true)
    List<Pot> findPotsByLocation(
            @Param("lon") Double lon,
            @Param("lat") Double lat,
            @Param("distance") Double distance);
}

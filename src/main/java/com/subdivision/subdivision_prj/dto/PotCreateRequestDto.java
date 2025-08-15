package com.subdivision.subdivision_prj.dto;

import com.subdivision.subdivision_prj.domain.Pot;
import com.subdivision.subdivision_prj.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.subdivision.subdivision_prj.domain.Pot.*;

@Getter
@Setter
@NoArgsConstructor
public class PotCreateRequestDto {

    private String title;
    private String content;
    private String productName;
    private int maximumHeadcount;
    private Double latitude;
    private Double longitude;

    //DTO를 Entity로 변환하는 메서드
    //이 메서드를 통해 Service 계층에서 DTO를 영속성(JPA)이 관리하는 Entity로 쉽게 변환할 수 있습니다.
    public Pot toEntity(User user) {
        return builder()
                .user(user) //작성자 정보
                .title(title)
                .content(content)
                .productName(productName)
                .maximumHeadcount(maximumHeadcount)
                .latitude(this.latitude)
                .longitude(this.longitude)
                .build();
    }
}

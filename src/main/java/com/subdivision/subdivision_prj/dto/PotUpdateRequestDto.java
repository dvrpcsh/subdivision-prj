package com.subdivision.subdivision_prj.dto;

import com.subdivision.subdivision_prj.domain.PotCategory;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class PotUpdateRequestDto {
    private String title;
    private String content;
    private String productName;
    private int maximumHeadcount;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private PotCategory category;
    private String address;
    private String detailAddress;
}

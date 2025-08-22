package com.subdivision.subdivision_prj.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationRequestDto {
    private String email;
    private String code;
}

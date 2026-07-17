package com.moyeo.controller.address;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "도로명주소 검색 요청")
public record AddressSearchRequest(
        @Schema(description = "도로명 또는 건물명을 포함한 검색어", example = "테헤란로 123")
        @NotBlank @Size(max = 100) String keyword
) { }

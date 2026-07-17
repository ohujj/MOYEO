package com.moyeo.controller.address;

import com.moyeo.address.AddressSearchService;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "도로명주소 검색 응답. 좌표 제공 API 승인 전까지 좌표는 포함하지 않습니다.")
public record AddressSearchResponse(
        @Schema(description = "검색 결과 총개수", example = "1") int totalCount,
        @Schema(description = "주소 목록") List<Address> addresses
) {
    public static AddressSearchResponse from(AddressSearchService.AddressSearchResult result) {
        return new AddressSearchResponse(result.totalCount(), result.addresses().stream().map(Address::from).toList());
    }
    @Schema(description = "좌표 조회에 필요한 주소 식별정보")
    public record Address(
            @Schema(description = "도로명주소", example = "서울특별시 강남구 테헤란로 123") String roadAddress,
            @Schema(description = "지번주소") String jibunAddress,
            @Schema(description = "우편번호", example = "06134") String zipCode,
            @Schema(description = "행정구역코드") String administrativeCode,
            @Schema(description = "도로명코드") String roadNameManagementNumber,
            @Schema(description = "지하여부. 0은 지상, 1은 지하") String underground,
            @Schema(description = "건물본번") String buildingMainNumber,
            @Schema(description = "건물부번") String buildingSubNumber
    ) {
        static Address from(AddressSearchService.AddressSearchResult.Address address) {
            return new Address(address.roadAddress(), address.jibunAddress(), address.zipCode(), address.administrativeCode(),
                    address.roadNameManagementNumber(), address.underground(), address.buildingMainNumber(), address.buildingSubNumber());
        }
    }
}

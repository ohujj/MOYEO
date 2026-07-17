package com.moyeo.controller.address;

import com.moyeo.address.AddressSearchService;
import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.member.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Address", description = "도로명주소 검색 API")
public class AddressSearchController {
    private final AddressSearchService addressSearchService;
    public AddressSearchController(AddressSearchService addressSearchService) { this.addressSearchService = addressSearchService; }

    @PostMapping("/searches")
    @Operation(summary = "도로명주소 검색", description = "로그인한 사용자가 도로명주소를 검색합니다. 좌표 제공 API 승인 전까지 응답에는 좌표가 포함되지 않습니다.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
            { "keyword": "테헤란로 123" }
            """))))
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "도로명주소 검색 성공"),
            @ApiResponse(responseCode = "400", description = "검색어 검증 실패", content = @Content(examples = @ExampleObject(value = """
                    { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                    """))),
            @ApiResponse(responseCode = "401", description = "Access Token 없음 또는 유효하지 않음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                    """))),
            @ApiResponse(responseCode = "503", description = "주소 검색 서비스 또는 승인키를 사용할 수 없음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "ADDRESS_SEARCH_UNAVAILABLE", "status": 503 }
                    """)))
    })
    public AddressSearchResponse search(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @Valid @RequestBody AddressSearchRequest request
    ) {
        return AddressSearchResponse.from(addressSearchService.search(request.keyword()));
    }
}

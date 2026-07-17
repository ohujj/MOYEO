package com.moyeo.address;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moyeo.global.error.MoyeoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
public class AddressSearchService {

    private static final Logger log = LoggerFactory.getLogger(AddressSearchService.class);

    private final RestClient restClient;
    private final AddressSearchProperties properties;

    public AddressSearchService(RestClient.Builder restClientBuilder, AddressSearchProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public AddressSearchResult search(String keyword) {
        if (properties.confirmationKey() == null || properties.confirmationKey().isBlank()) {
            log.warn("Juso address search is unavailable because JUSO_SEARCH_CONFM_KEY is not configured.");
            throw unavailable();
        }

        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .queryParam("confmKey", properties.confirmationKey())
                .queryParam("currentPage", 1)
                .queryParam("countPerPage", 10)
                .queryParam("keyword", keyword.strip())
                .queryParam("resultType", "json")
                .build()
                .encode()
                .toUri();
        try {
            JusoResponse response = restClient.get().uri(uri).retrieve().body(JusoResponse.class);
            if (response == null || response.results() == null || response.results().common() == null) {
                log.warn("Juso address search returned an unreadable response.");
                throw unavailable();
            }
            if (!"0".equals(response.results().common().errorCode())) {
                log.warn(
                        "Juso address search failed: errorCode={}, errorMessage={}",
                        response.results().common().errorCode(),
                        response.results().common().errorMessage()
                );
                throw unavailable();
            }
            List<AddressSearchResult.Address> addresses = response.results().juso() == null ? List.of()
                    : response.results().juso().stream()
                    .map(address -> new AddressSearchResult.Address(
                            address.roadAddr(), address.jibunAddr(), address.zipNo(), address.admCd(),
                            address.rnMgtSn(), address.udrtYn(), address.buldMnnm(), address.buldSlno()
                    ))
                    .toList();
            return new AddressSearchResult(response.results().common().parsedTotalCount(), addresses);
        } catch (RestClientException exception) {
            log.warn("Juso address search request failed.", exception);
            throw unavailable();
        }
    }

    private MoyeoException unavailable() {
        return new MoyeoException(AddressSearchErrorCode.ADDRESS_SEARCH_UNAVAILABLE);
    }

    public record AddressSearchResult(int totalCount, List<Address> addresses) {
        public record Address(String roadAddress, String jibunAddress, String zipCode, String administrativeCode,
                              String roadNameManagementNumber, String underground, String buildingMainNumber,
                              String buildingSubNumber) { }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JusoResponse(Results results) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Results(Common common, List<Juso> juso) { }
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Common(String totalCount, String errorCode, String errorMessage) {
            int parsedTotalCount() { return totalCount == null || totalCount.isBlank() ? 0 : Integer.parseInt(totalCount); }
        }
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Juso(String roadAddr, String jibunAddr, String zipNo, String admCd, String rnMgtSn,
                            String udrtYn, String buldMnnm, String buldSlno) { }
    }
}

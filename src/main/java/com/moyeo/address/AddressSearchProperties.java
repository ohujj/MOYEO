package com.moyeo.address;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "moyeo.address-search")
public record AddressSearchProperties(
        String baseUrl,
        String confirmationKey
) {
}

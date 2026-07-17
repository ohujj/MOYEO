package com.moyeo.global.security;

import com.moyeo.global.CorsProperties;
import com.moyeo.address.AddressSearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, AddressSearchProperties.class})
public class JwtConfig {
}

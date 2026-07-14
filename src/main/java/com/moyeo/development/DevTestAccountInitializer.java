package com.moyeo.development;

import com.moyeo.service.member.MemberAuthService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "dev"})
class DevTestAccountInitializer {

    @Bean
    ApplicationRunner initializeDevTestAccounts(MemberAuthService memberAuthService) {
        return arguments -> {
            for (DevTestAccount account : DevTestAccount.values()) {
                account.createIfMissing(memberAuthService);
            }
        };
    }
}

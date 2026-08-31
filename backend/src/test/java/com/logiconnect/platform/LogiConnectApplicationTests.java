package com.logiconnect.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LogiConnectApplicationTests {

    @Test
    @DisplayName("Spring context loads successfully")
    void contextLoads() {
        // Assert context startup completes without bean wiring or configuration errors
    }

    @Test
    @DisplayName("JVM default timezone is initialized to UTC")
    void verifyDefaultTimezoneIsUtc() {
        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
    }
}

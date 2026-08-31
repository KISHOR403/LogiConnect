
package com.logiconnect.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Test
    @DisplayName("BCrypt encoder securely hashes and matches raw passwords")
    void testPasswordEncodingAndMatching() {
        String rawPassword = "LogiConnectSecurePassword#2026";
        String encodedHash = passwordEncoder.encode(rawPassword);

        assertThat(encodedHash).isNotBlank();
        assertThat(encodedHash).isNotEqualTo(rawPassword);
        assertThat(encodedHash).startsWith("$2a$12$");

        assertThat(passwordEncoder.matches(rawPassword, encodedHash)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword#123", encodedHash)).isFalse();
    }
}

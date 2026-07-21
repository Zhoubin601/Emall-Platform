package com.emall.backend.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void passwordCanBeReceivedButIsNeverSerialized() throws Exception {
        User requestUser = objectMapper.readValue(
                "{\"username\":\"tester\",\"password\":\"secret-value\"}",
                User.class);
        assertThat(requestUser.getPassword()).isEqualTo("secret-value");

        String responseJson = objectMapper.writeValueAsString(requestUser);
        assertThat(responseJson).doesNotContain("password", "secret-value");
    }
}

package com.unid8.damideo.common.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unid8.damideo.common.response.ApiResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(DeviceIdTest.SampleController.class)
@Import({DeviceIdTest.SampleController.class, DeviceIdTest.SampleConfig.class})
class DeviceIdTest {

    private static final String REGISTERED = "3f2504e0-4f89-41d3-9a0c-0305e82c3301";
    private static final String UNREGISTERED = "9b2c1f7a-1d2e-4c3b-8a9f-111111111111";

    @Autowired
    MockMvc mockMvc;

    @Test
    void 등록된_기기면_사용자_id를_받는다() throws Exception {
        mockMvc.perform(get("/api/sample/me").header(DeviceId.HEADER, REGISTERED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void 헤더가_없으면_401_USER_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/sample/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void 형식이_UUID가_아니면_400_INVALID_INPUT() throws Exception {
        mockMvc.perform(get("/api/sample/me").header(DeviceId.HEADER, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors['X-Device-Id']").exists());
    }

    @Test
    void 등록되지_않은_기기면_401_USER_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/sample/me").header(DeviceId.HEADER, UNREGISTERED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void 게스트_생성_주소는_헤더_없이_통과한다() throws Exception {
        mockMvc.perform(post("/api/users"))
                .andExpect(status().isOk());
    }

    @RestController
    static class SampleController {

        @GetMapping("/api/sample/me")
        ApiResponse<Long> me(@CurrentUser Long userId) {
            return ApiResponse.ok("OK", "확인", userId);
        }

        @PostMapping("/api/users")
        ApiResponse<Void> create() {
            return ApiResponse.ok("OK", "생성", null);
        }
    }

    @TestConfiguration
    static class SampleConfig {

        @Bean
        DeviceUserReader deviceUserReader() {
            return deviceId -> REGISTERED.equals(deviceId.toString()) ? Optional.of(1L) : Optional.empty();
        }
    }
}

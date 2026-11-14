package com.unid8.damideo.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unid8.damideo.TestcontainersConfiguration;
import com.unid8.damideo.common.auth.DeviceUserReader;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserApiTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DeviceUserReader deviceUserReader;

    @Test
    void 새_기기면_계정을_만든다() throws Exception {
        String deviceId = UUID.randomUUID().toString();
        String nickname = nickname();

        create(deviceId, nickname)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_CREATED"))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.created").value(true));

        assertThat(userRepository.findByDeviceId(deviceId)).isPresent();
    }

    @Test
    void 이미_등록된_기기면_기존_계정을_돌려준다() throws Exception {
        String deviceId = UUID.randomUUID().toString();
        String nickname = nickname();
        create(deviceId, nickname).andExpect(status().isOk());

        create(deviceId, nickname())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_FOUND"))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.created").value(false));
    }

    @Test
    void 닉네임이_중복이면_409() throws Exception {
        String nickname = nickname();
        create(UUID.randomUUID().toString(), nickname).andExpect(status().isOk());

        create(UUID.randomUUID().toString(), nickname)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NICKNAME_DUPLICATED"));
    }

    @Test
    void 닉네임_길이가_맞지_않으면_400() throws Exception {
        create(UUID.randomUUID().toString(), "가")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors.nickname").value("2~10자로 입력해주세요."));

        create(UUID.randomUUID().toString(), "가나다라마바사아자차카")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nickname").exists());
    }

    @Test
    void 기기_ID가_UUID가_아니면_400() throws Exception {
        create("not-a-uuid", nickname())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.deviceId").exists());
    }

    @Test
    void 만든_계정은_기기_ID로_찾을_수_있다() throws Exception {
        UUID deviceId = UUID.randomUUID();
        create(deviceId.toString().toUpperCase(), nickname()).andExpect(status().isOk());

        Long userId = userRepository.findByDeviceId(deviceId.toString()).orElseThrow().getId();
        assertThat(deviceUserReader.findUserIdByDeviceId(deviceId)).contains(userId);
        assertThat(deviceUserReader.findUserIdByDeviceId(UUID.randomUUID())).isEmpty();
    }

    private ResultActions create(String deviceId, String nickname) throws Exception {
        String body = """
                {"deviceId": "%s", "nickname": "%s"}
                """.formatted(deviceId, nickname);
        return mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String nickname() {
        return "테스터" + SEQ.incrementAndGet();
    }
}

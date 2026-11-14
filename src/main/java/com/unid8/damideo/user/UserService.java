package com.unid8.damideo.user;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.exception.CommonErrorCode;
import com.unid8.damideo.user.dto.CreateUserResponse;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 같은 기기로 이미 만든 계정이 있으면 그대로 돌려주고, 없으면 새로 만든다.
     */
    @Transactional
    public CreateUserResponse createGuest(String rawDeviceId, String rawNickname) {
        String deviceId = rawDeviceId.toLowerCase(Locale.ROOT);
        String nickname = rawNickname.strip();
        if (nickname.length() < 2) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, Map.of("nickname", "2~10자로 입력해주세요."));
        }

        Optional<User> existing = userRepository.findByDeviceId(deviceId);
        if (existing.isPresent()) {
            User user = existing.get();
            return new CreateUserResponse(user.getId(), user.getNickname(), false);
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
        }

        try {
            User saved = userRepository.saveAndFlush(User.createGuest(deviceId, nickname));
            return new CreateUserResponse(saved.getId(), saved.getNickname(), true);
        } catch (DataIntegrityViolationException e) {
            // 같은 닉네임이나 기기로 동시에 가입한 경우
            throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
        }
    }
}

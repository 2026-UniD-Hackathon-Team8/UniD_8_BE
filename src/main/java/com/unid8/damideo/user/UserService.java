package com.unid8.damideo.user;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.exception.CommonErrorCode;
import com.unid8.damideo.user.dto.CreateUserResponse;
import com.unid8.damideo.user.dto.NicknameResponse;
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
        String nickname = normalizeNickname(rawNickname);

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

    /**
     * 닉네임을 바꾼다. 지금 닉네임과 같으면 그대로 둔다.
     */
    @Transactional
    public NicknameResponse changeNickname(Long userId, String rawNickname) {
        String nickname = normalizeNickname(rawNickname);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
        if (nickname.equals(user.getNickname())) {
            return new NicknameResponse(nickname);
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
        }

        user.changeNickname(nickname);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
        }
        return new NicknameResponse(nickname);
    }

    private static String normalizeNickname(String rawNickname) {
        String nickname = rawNickname.strip();
        if (nickname.length() < 2) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, Map.of("nickname", "2~10자로 입력해주세요."));
        }
        return nickname;
    }
}

package com.unid8.damideo.user;

import com.unid8.damideo.common.auth.DeviceUserReader;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserDeviceReader implements DeviceUserReader {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findUserIdByDeviceId(UUID deviceId) {
        return userRepository.findByDeviceId(deviceId.toString()).map(User::getId);
    }
}

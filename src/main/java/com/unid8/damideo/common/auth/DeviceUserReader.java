package com.unid8.damideo.common.auth;

import java.util.Optional;
import java.util.UUID;

/**
 * 기기 UUID로 사용자 id를 찾는다. user 도메인에서 구현한다.
 */
public interface DeviceUserReader {

    Optional<Long> findUserIdByDeviceId(UUID deviceId);
}

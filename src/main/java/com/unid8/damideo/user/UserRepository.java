package com.unid8.damideo.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByDeviceId(String deviceId);

    boolean existsByNickname(String nickname);

    List<User> findAllByBestStreakWinGreaterThan(int wins);
}

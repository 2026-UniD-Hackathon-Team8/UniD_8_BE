package com.unid8.damideo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 36, unique = true)
    private String deviceId;

    @Column(nullable = false, length = 50, unique = true)
    private String nickname;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_completed_date")
    private LocalDate lastCompletedDate;

    @Column(name = "best_streak_win", nullable = false)
    private int bestStreakWin;

    @Column(name = "best_streak_win_at")
    private LocalDateTime bestStreakWinAt;

    @Column(name = "current_type", length = 30)
    private String currentType;

    @Column(name = "candidate_type", length = 30)
    private String candidateType;

    @Column(name = "candidate_since")
    private LocalDate candidateSince;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private User(String deviceId, String nickname) {
        this.deviceId = deviceId;
        this.nickname = nickname;
    }

    public static User createGuest(String deviceId, String nickname) {
        return new User(deviceId, nickname);
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}

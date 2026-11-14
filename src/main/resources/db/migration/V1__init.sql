-- 1. 사용자
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 PK',
    device_id VARCHAR(36) NOT NULL COMMENT '기기 UUID (X-Device-Id)',
    nickname VARCHAR(50) NOT NULL COMMENT '유니크 닉네임',
    current_streak INT NOT NULL DEFAULT 0 COMMENT '현재 데일리 연속 일수',
    longest_streak INT NOT NULL DEFAULT 0 COMMENT '최장 데일리 연속 일수',
    last_completed_date DATE NULL COMMENT '마지막 데일리 완료일 (KST)',
    best_streak_win INT NOT NULL DEFAULT 0 COMMENT '연승 챌린지 최고 연승 기록',
    best_streak_win_at DATETIME NULL COMMENT '최고 연승 달성 시각 (UTC)',
    current_type VARCHAR(30) NULL COMMENT '현재 약점 유형 코드',
    candidate_type VARCHAR(30) NULL COMMENT '변경 후보 유형 코드',
    candidate_since DATE NULL COMMENT '후보 유형 시작일 (KST)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각 (UTC)',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각 (UTC)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_device_id (device_id),
    UNIQUE KEY uk_users_nickname (nickname),
    KEY idx_users_streak_ranking (best_streak_win DESC, best_streak_win_at ASC)
) COMMENT = '사용자';

-- 2. 사용자 약점 유형 변경 이력 (성장 탭 - 지나온 유형)
CREATE TABLE user_type_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '유형 이력 PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    type_code VARCHAR(30) NOT NULL COMMENT '확정된 약점 유형 코드',
    recorded_date DATE NOT NULL COMMENT '유형 변경 확정일 (KST)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각 (UTC)',
    PRIMARY KEY (id),
    CONSTRAINT fk_user_type_history_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT = '사용자 약점 유형 변경 이력';

-- 3. 문제
CREATE TABLE questions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '문제 PK',
    category VARCHAR(30) NOT NULL COMMENT '계열 코드 (EVIDENCE, CONTEXT, LOGIC, TIMELINE, QUESTION)',
    tactic VARCHAR(50) NOT NULL COMMENT '수법 코드 (FAKE_SOURCE 등)',
    question_type VARCHAR(30) NOT NULL COMMENT '문제 유형 3종 코드',
    prompt TEXT NOT NULL COMMENT '질문 및 상황 (대화 고르기 앞부분 포함)',
    credible_content TEXT NOT NULL COMMENT '믿을 쪽 보기 내용',
    target_content TEXT NOT NULL COMMENT '골라낼 쪽(정답) 보기 내용',
    credible_ai_answer TEXT NULL COMMENT '믿을 쪽 AI 답 (유형별 선택)',
    target_ai_answer TEXT NULL COMMENT '골라낼 쪽 AI 답 (유형별 선택)',
    explanation TEXT NOT NULL COMMENT '해설',
    check_question TEXT NOT NULL COMMENT '확인 질문',
    source VARCHAR(1000) NOT NULL COMMENT '출처 (한글 URL 인코딩 고려)',
    difficulty TINYINT NOT NULL COMMENT '난이도',
    is_daily_eligible BOOLEAN NOT NULL DEFAULT TRUE COMMENT '데일리 출제 가능 여부',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각 (UTC)',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각 (UTC)',
    PRIMARY KEY (id)
) COMMENT = '문제';

-- 4. 데일리 세트 (날짜별 공통 1세트)
CREATE TABLE daily_sets (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '데일리 세트 PK',
    daily_date DATE NOT NULL COMMENT '데일리 날짜 (KST)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각 (UTC)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_sets_date (daily_date)
) COMMENT = '데일리 세트';

-- 5. 데일리 세트 출제 문제 매핑
CREATE TABLE daily_set_questions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '데일리 세트 문제 PK',
    daily_set_id BIGINT NOT NULL COMMENT '데일리 세트 ID',
    question_id BIGINT NOT NULL COMMENT '문제 ID',
    seq TINYINT NOT NULL COMMENT '출제 순서 (1~5: 근거->맥락->논리->시점->질문)',
    answer_pos CHAR(1) NOT NULL COMMENT '골라낼 쪽(정답) 위치 (A/B)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_set_questions_seq (daily_set_id, seq),
    UNIQUE KEY uk_daily_set_questions_question (daily_set_id, question_id),
    CONSTRAINT fk_daily_set_questions_set FOREIGN KEY (daily_set_id) REFERENCES daily_sets (id),
    CONSTRAINT fk_daily_set_questions_question FOREIGN KEY (question_id) REFERENCES questions (id)
) COMMENT = '데일리 세트 출제 문제';

-- 6. 일반/약점 재도전 게임 판
CREATE TABLE game_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '게임 판 PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    game_mode VARCHAR(30) NOT NULL COMMENT '판 모드 (NORMAL, WEAKNESS_RETRY)',
    status VARCHAR(20) NOT NULL COMMENT '판 상태 (IN_PROGRESS, COMPLETED, ABANDONED)',
    total_questions INT NOT NULL DEFAULT 10 COMMENT '판 총 문제 수',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시작 시각 (UTC)',
    completed_at DATETIME NULL COMMENT '완료 시각 (UTC)',
    PRIMARY KEY (id),
    CONSTRAINT fk_game_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT = '게임 판';

-- 7. 게임 판 출제 문제 매핑
CREATE TABLE game_session_questions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '게임 판 문제 PK',
    game_session_id BIGINT NOT NULL COMMENT '게임 판 ID',
    question_id BIGINT NOT NULL COMMENT '문제 ID',
    seq INT NOT NULL COMMENT '출제 순서 (1~N)',
    answer_pos CHAR(1) NOT NULL COMMENT '골라낼 쪽(정답) 위치 (A/B)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_session_questions_seq (game_session_id, seq),
    UNIQUE KEY uk_game_session_questions_question (game_session_id, question_id),
    CONSTRAINT fk_game_session_questions_session FOREIGN KEY (game_session_id) REFERENCES game_sessions (id),
    CONSTRAINT fk_game_session_questions_question FOREIGN KEY (question_id) REFERENCES questions (id)
) COMMENT = '게임 판 출제 문제';

-- 8. 연승 챌린지 도전
CREATE TABLE streak_challenges (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '연승 도전 PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    status VARCHAR(20) NOT NULL COMMENT '도전 상태 (IN_PROGRESS, ENDED)',
    win_count INT NOT NULL DEFAULT 0 COMMENT '현재/최종 연승 수',
    current_question_id BIGINT NULL COMMENT '현재 출제 중인 문제 ID',
    current_answer_pos CHAR(1) NULL COMMENT '현재 문제 정답 위치 (A/B)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시작 시각 (UTC)',
    ended_at DATETIME NULL COMMENT '종료 시각 (UTC)',
    PRIMARY KEY (id),
    CONSTRAINT fk_streak_challenges_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_streak_challenges_current_question FOREIGN KEY (current_question_id) REFERENCES questions (id)
) COMMENT = '연승 챌린지';

-- 9. 통합 답안 기록
CREATE TABLE answers (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '답안 PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    question_id BIGINT NOT NULL COMMENT '문제 ID',
    source VARCHAR(20) NOT NULL COMMENT '풀이 출처 (DAILY, GAME, STREAK)',
    game_session_id BIGINT NULL COMMENT '게임 판 ID (GAME일 때 존재)',
    streak_challenge_id BIGINT NULL COMMENT '연승 도전 ID (STREAK일 때 존재)',
    daily_date DATE NULL COMMENT '데일리 날짜 KST (DAILY일 때 존재)',
    selected_pos CHAR(1) NOT NULL COMMENT '사용자가 고른 답 (A/B)',
    is_correct BOOLEAN NOT NULL COMMENT '정답 여부',
    first_attempt BOOLEAN NOT NULL COMMENT '최초 풀이 여부',
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '제출 시각 (UTC)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_answers_game (game_session_id, question_id),
    UNIQUE KEY uk_answers_streak (streak_challenge_id, question_id),
    UNIQUE KEY uk_answers_daily (user_id, daily_date, question_id),
    KEY idx_answers_user_first_submitted (user_id, first_attempt, submitted_at),
    KEY idx_answers_user_question (user_id, question_id, submitted_at),
    CONSTRAINT fk_answers_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_answers_game_session FOREIGN KEY (game_session_id) REFERENCES game_sessions (id),
    CONSTRAINT fk_answers_streak_challenge FOREIGN KEY (streak_challenge_id) REFERENCES streak_challenges (id)
) COMMENT = '통합 답안 기록';

-- 10. 오답 메모
CREATE TABLE wrong_answer_notes (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '오답 메모 PK',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    question_id BIGINT NOT NULL COMMENT '문제 ID',
    memo TEXT NOT NULL COMMENT '오답 메모 내용',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각 (UTC)',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각 (UTC)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wrong_answer_notes_user_question (user_id, question_id),
    CONSTRAINT fk_wrong_answer_notes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_wrong_answer_notes_question FOREIGN KEY (question_id) REFERENCES questions (id)
) COMMENT = '오답 메모';
